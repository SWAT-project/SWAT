package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.common.Util
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation
import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor
import de.uzl.its.swat.symbolic.instruction.GETVALUE_Object
import de.uzl.its.swat.symbolic.instruction.Instruction
import de.uzl.its.swat.symbolic.instruction.NOP
import de.uzl.its.swat.symbolic.processor.BaseSymbolicInstructionProcessorSpec
import de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import spock.lang.See

/**
 * G3 register-only-non-constant (Level L1). At the value-typed GETVALUE recovery, a de-interned value
 * type (String or a cached boxed wrapper) whose shadow carries symbolic content (a variable/UF) IS
 * heap-registered so it round-trips through untracked space (the whitelisted-pure-UF win), while a
 * pure-constant shadow is NOT registered — it is reconstructible from the observed concrete, so
 * registering it would only grow the heap. G3 de-interning (bytecode) makes the registered reference
 * sound; this spec pins the executor-side registration policy for String (A1) and boxed (A2), and that
 * mutable objects on the shared boxed/mutable recovery path keep unconditional registration. See
 * docs/heap-redesign-g3-design.md.
 */
class OutputDeInternSpec extends BaseSymbolicInstructionProcessorSpec {

    /** Recover an object at ADDRESS_UNKNOWN: push the shadow, then drive GETVALUE_Object. */
    private void recover(Value shadow, Object obj) {
        SymbolicInstructionVisitor visitor = ThreadHandler.getSymbolicVisitor(threadId)
        visitor.getStack().pushOperand(shadow)
        List<Instruction> instructions = new ArrayList<>()
        instructions.add(new GETVALUE_Object(GlobalStateForInstrumentation.instance.incAndGetId(),
                System.identityHashCode(obj), obj, 0))
        instructions.add(new NOP(GlobalStateForInstrumentation.instance.incAndGetId()))
        // setCurrent(first); processing the next visits the *current* (execution is one behind).
        ThreadHandler.setCurrentInstruction(threadId, instructions.remove(0))
        SymbolicInstructionProcessor processor = new SymbolicInstructionProcessor()
        while (!instructions.isEmpty()) {
            processor.processInstruction(instructions.remove(0))
        }
    }

    @See("docs/heap-redesign-g3-design.md")
    def "G3: a symbolic String shadow IS heap-registered (round-trip enabled)"() {
        given: "a symbolic String shadow awaiting its address (ADDRESS_UNKNOWN)"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        String obj = "symbolic-abc"
        StringValue shadow = new StringValue(solverContext, obj, ObjectValue.ADDRESS_UNKNOWN)
        shadow.MAKE_SYMBOLIC()

        when: "the value is recovered"
        recover(shadow, obj)

        then: "it is registered, so an untracked round-trip can recover the symbolic shadow"
        ThreadHandler.getSymbolicVisitor(threadId).getStack().getFromHeap(obj) != null
    }

    @See("docs/heap-redesign-g3-design.md")
    def "G3: a constant String shadow is NOT heap-registered (reconstructible; no leak)"() {
        given: "a pure-constant String shadow (formula = makeString) awaiting its address"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        String obj = "constant-xyz"
        StringValue shadow = new StringValue(solverContext, obj, ObjectValue.ADDRESS_UNKNOWN)

        when: "the value is recovered"
        recover(shadow, obj)

        then: "it is NOT registered - reconstructible from the observed concrete, so no leak"
        ThreadHandler.getSymbolicVisitor(threadId).getStack().getFromHeap(obj) == null
    }

    @See("docs/heap-redesign-g3-design.md")
    def "G3-A2: a symbolic boxed shadow IS heap-registered (round-trip enabled)"() {
        given: "a symbolic Integer shadow awaiting its address (formula carried by the inner IntValue)"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        Integer obj = 7
        IntValue inner = new IntValue(solverContext, obj)
        inner.MAKE_SYMBOLIC()
        IntegerObjectValue shadow = new IntegerObjectValue(solverContext, inner, ObjectValue.ADDRESS_UNKNOWN)

        when: "the value is recovered"
        recover(shadow, obj)

        then: "it is registered, so an untracked round-trip can recover the symbolic boxed shadow"
        ThreadHandler.getSymbolicVisitor(threadId).getStack().getFromHeap(obj) != null
    }

    @See("docs/heap-redesign-g3-design.md")
    def "G3-A2: a constant boxed shadow is NOT heap-registered (reconstructible; no leak)"() {
        given: "a pure-constant Integer shadow awaiting its address"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        Integer obj = 7
        IntegerObjectValue shadow =
                new IntegerObjectValue(solverContext, new IntValue(solverContext, obj), ObjectValue.ADDRESS_UNKNOWN)

        when: "the value is recovered"
        recover(shadow, obj)

        then: "it is NOT registered - a constant boxed value is reconstructible from the observed concrete"
        ThreadHandler.getSymbolicVisitor(threadId).getStack().getFromHeap(obj) == null
    }

    @See("docs/heap-redesign-g3-design.md")
    def "G3-A2: a mutable (non-value-type) object is ALWAYS registered (shared path unaffected)"() {
        given: "a plain mutable object whose class is not de-interned"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        Object obj = new Object()
        ObjectValue shadow = new ObjectValue(solverContext, ObjectValue.ADDRESS_UNKNOWN)

        when: "the value is recovered"
        recover(shadow, obj)

        then: "register-only-non-constant must NOT touch mutable objects - sound identity key, must track"
        ThreadHandler.getSymbolicVisitor(threadId).getStack().getFromHeap(obj) != null
    }
}
