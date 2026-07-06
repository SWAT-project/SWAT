package de.uzl.its.swat.symbolic.processor

import de.uzl.its.swat.instrument.GlobalStateForInstrumentation
import de.uzl.its.swat.symbolic.instruction.INVOKEMETHOD_EXCEPTION
import de.uzl.its.swat.symbolic.instruction.INVOKESTATIC
import de.uzl.its.swat.symbolic.instruction.INVOKEVIRTUAL
import de.uzl.its.swat.symbolic.instruction.Instruction
import de.uzl.its.swat.symbolic.instruction.NOP
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler

/**
 * A non-instrumented callee that THROWS never reaches the normal-return invoke handling
 * (INVOKEMETHOD_END); the trace continues with INVOKEMETHOD_EXCEPTION instead. If such a call
 * depended on a symbolic input, whether the exception was raised is unmodeled control flow, so the
 * invoke handlers must record symbolic context loss (blocking a SAFE verdict) - and must NOT flag
 * when all inputs are concrete or the callee is ignored plumbing.
 */
class ExceptionalInvokeContextLossSpec extends BaseSymbolicInstructionProcessorSpec {

    /**
     * Exception-path fixture: operands must already be pushed (receiver first for an instance
     * call, then arguments); processes INVOKE(owner,name,desc) -> INVOKEMETHOD_EXCEPTION (the
     * callee threw: no return value, no INVOKEMETHOD_END). Returns whether symbolic context loss
     * was recorded. Both instructions share the invokeId so the open invoke closes and the
     * exception instruction is visited despite the callee frame's class differing (mirrors
     * executeBoundaryRecovery).
     */
    private boolean executeThrowingInvoke(String owner, String name, String desc, boolean isInstance) {
        List<Instruction> instructions = new ArrayList<>()
        long invokeId = GlobalStateForInstrumentation.instance.incAndGetInvokeId()
        Instruction invoke = isInstance
                ? new INVOKEVIRTUAL(GlobalStateForInstrumentation.instance.incAndGetId(), invokeId, owner, name, desc)
                : new INVOKESTATIC(GlobalStateForInstrumentation.instance.incAndGetId(), invokeId, owner, name, desc)
        instructions.add(invoke)
        instructions.add(new INVOKEMETHOD_EXCEPTION(GlobalStateForInstrumentation.instance.incAndGetId(), invokeId))
        // The trailing NOP flushes the INVOKEMETHOD_EXCEPTION visit (execution is one behind).
        instructions.add(new NOP(GlobalStateForInstrumentation.instance.incAndGetId()))

        ThreadHandler.setCurrentInstruction(threadId, instructions.remove(0))
        SymbolicInstructionProcessor processor = new SymbolicInstructionProcessor()
        while (!instructions.isEmpty()) {
            processor.processInstruction(instructions.remove(0))
        }
        return ThreadHandler.getSymbolicTraceHandler(threadId).isSymbolicContextLoss()
    }

    def "a throwing static call with a symbolic argument records context loss"() {
        given: "a symbolic String argument into a throwing static JDK call (the Float.parseFloat shape)"
        setupTestContext("test/ThrowingStatic", "test")
        def arg = pushStringOperand("fixed")
        arg.MAKE_SYMBOLIC()

        when: "the callee throws instead of returning"
        def loss = executeThrowingInvoke("java/lang/Float", "parseFloat", "(Ljava/lang/String;)F", false)

        then: "the unexplored throw decision blocks SAFE"
        loss
    }

    def "a throwing static call with only concrete inputs records no context loss"() {
        given: "a concrete String argument"
        setupTestContext("test/ThrowingStaticConcrete", "test")
        pushStringOperand("fixed")

        when:
        def loss = executeThrowingInvoke("java/lang/Float", "parseFloat", "(Ljava/lang/String;)F", false)

        then: "concrete-only calls never flag (no over-approximation)"
        !loss
    }

    def "a throwing call on an ignored owner records no context loss even with symbolic input"() {
        given: "a symbolic argument into ignored I/O plumbing"
        setupTestContext("test/ThrowingIgnored", "test")
        def arg = pushStringOperand("fixed")
        arg.MAKE_SYMBOLIC()

        when:
        def loss = executeThrowingInvoke("java/util/Scanner", "next", "(Ljava/lang/String;)Ljava/lang/String;", false)

        then: "parity with the normal path's IGNORED_INVOCATIONS exemption"
        !loss
    }

    def "a symbolic argument at a non-zero index is found (concrete first, symbolic second)"() {
        given: "a concrete int then a symbolic float"
        setupTestContext("test/ThrowingMixed", "test")
        pushPrimitiveOperand((int) 7)
        def f = pushPrimitiveOperand((float) 1.5f)
        f.MAKE_SYMBOLIC()

        when:
        def loss = executeThrowingInvoke("java/lang/Math", "someOp", "(IF)F", false)

        then:
        loss
    }

    def "a symbolic wide (category-2) argument is found before a concrete trailing one"() {
        given: "a symbolic long then a concrete float (exercises the wide-local offset)"
        setupTestContext("test/ThrowingWide", "test")
        def l = pushPrimitiveOperand((long) 42L)
        l.MAKE_SYMBOLIC()
        pushPrimitiveOperand((float) 1.5f)

        when:
        def loss = executeThrowingInvoke("java/lang/Math", "someOp", "(JF)F", false)

        then:
        loss
    }

    def "a throwing instance call with a symbolic receiver records context loss"() {
        given: "a symbolic receiver and a concrete argument"
        setupTestContext("test/ThrowingVirtual", "test")
        def receiver = new StringValue(solverContext, "abcd", ObjectValue.ADDRESS_UNKNOWN)
        receiver.MAKE_SYMBOLIC()
        pushOperand(receiver)
        pushPrimitiveOperand((int) 99)

        when: "e.g. substring(99) throwing out of range"
        def loss = executeThrowingInvoke("java/lang/String", "substring", "(I)Ljava/lang/String;", true)

        then:
        loss
    }

    def "a throwing instance call with concrete receiver and arguments records no context loss"() {
        given:
        setupTestContext("test/ThrowingVirtualConcrete", "test")
        pushOperand(new StringValue(solverContext, "abcd", ObjectValue.ADDRESS_UNKNOWN))
        pushPrimitiveOperand((int) 99)

        when:
        def loss = executeThrowingInvoke("java/lang/String", "substring", "(I)Ljava/lang/String;", true)

        then:
        !loss
    }
}
