package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.common.Util
import de.uzl.its.swat.config.Config
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation
import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor
import de.uzl.its.swat.symbolic.instruction.GETVALUE_int
import de.uzl.its.swat.symbolic.instruction.Instruction
import de.uzl.its.swat.symbolic.instruction.NOP
import de.uzl.its.swat.symbolic.processor.BaseSymbolicInstructionProcessorSpec
import de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
import de.uzl.its.swat.symbolic.shadow.ShadowDivergence
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.thread.ThreadHandler

/**
 * Out-of-band change detection at a primitive GETVALUE sync. When the value the real JVM produced
 * diverges from the tracked shadow's concrete (for example, a field mutated inside unmodeled code), the
 * {@code shadowDivergence=FLAG} policy handles it gracefully: it records context loss, adopts the
 * observed concrete, and does not throw, unlike the CRASH policy's hard assert.
 */
class OutOfBandDetectionSpec extends BaseSymbolicInstructionProcessorSpec {

    private ShadowDivergence savedPolicy

    def setup() {
        savedPolicy = Config.instance().getShadowDivergence()
    }

    def cleanup() {
        Config.instance().setShadowDivergence(savedPolicy)
    }

    /** Drives a single GETVALUE_int(observed) over the current operand-stack top. */
    private void runGetValueInt(int observed) {
        List<Instruction> instructions = new ArrayList<>()
        instructions.add(new GETVALUE_int(GlobalStateForInstrumentation.instance.incAndGetId(), observed, 0))
        instructions.add(new NOP(GlobalStateForInstrumentation.instance.incAndGetId()))
        // setCurrent(first); processing the next visits the *current* (execution is one behind).
        ThreadHandler.setCurrentInstruction(threadId, instructions.remove(0))
        SymbolicInstructionProcessor processor = new SymbolicInstructionProcessor()
        while (!instructions.isEmpty()) {
            processor.processInstruction(instructions.remove(0))
        }
    }

    def "under FLAG a diverging primitive GETVALUE is flagged and re-grounded, not crashed"() {
        given: "a tracked int shadow with concrete 10, under the FLAG policy"
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        Config.instance().setShadowDivergence(ShadowDivergence.FLAG)
        SymbolicInstructionVisitor visitor = ThreadHandler.getSymbolicVisitor(threadId)
        visitor.getStack().pushOperand(new IntValue(solverContext, 10))

        when: "the real JVM produced a different value (20) for that slot - an out-of-band change"
        runGetValueInt(20)

        then: "no crash; context loss flagged; the operand is re-grounded to the observed concrete"
        ThreadHandler.getSymbolicTraceHandler(threadId).isSymbolicContextLoss()
        visitor.getStack().getActiveFrame().peek().concrete == 20
    }

    def "under FLAG a matching primitive GETVALUE records no context-loss flag"() {
        given:
        setupTestContext(Util.formatClassName("de.uzl.its.swat.test.TestClass"), "main")
        Config.instance().setShadowDivergence(ShadowDivergence.FLAG)
        SymbolicInstructionVisitor visitor = ThreadHandler.getSymbolicVisitor(threadId)
        visitor.getStack().pushOperand(new IntValue(solverContext, 10))

        when: "the observed value matches the shadow"
        runGetValueInt(10)

        then: "no divergence, so no context-loss flag"
        !ThreadHandler.getSymbolicTraceHandler(threadId).isSymbolicContextLoss()
    }
}
