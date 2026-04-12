package de.uzl.its.swat.symbolic.processor


import de.uzl.its.swat.symbolic.instruction.I2F
import de.uzl.its.swat.symbolic.instruction.IADD

// Assumed conversion instruction.
import de.uzl.its.swat.symbolic.instruction.Instruction
import de.uzl.its.swat.symbolic.instruction.NOP
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.thread.ThreadHandler

class IADDSpec extends BaseSymbolicInstructionProcessorSpec {

    def "symbolic IADD instruction test"() {
        given: "A stack with two integer operands"
        String testClassName = "de.uzl.its.swat.test.TestClass"
        String testMethodName = "main"
        def contextData = setupTestContext(testClassName, testMethodName)
        long threadId = contextData.threadId
        int classIndex = contextData.classIndex
        int methodIdx = contextData.methodIdx
        def visitor = contextData.visitor

        // Push an integer operand (e.g., 42) onto the active frame.
        pushPrimitiveOperand(42)
        pushPrimitiveOperand(10)
        and: "An IADD instruction"
        // Create an instruction ID (iid) using classIndex, methodIdx and an arbitrary offset.
        long iid = (classIndex << 35) | (methodIdx << 24) | 1
        Instruction inst = new IADD(iid)

        // Set the current instruction in the ThreadHandler.
        ThreadHandler.setCurrentInstruction(threadId, inst)

        SymbolicInstructionProcessor processor = new SymbolicInstructionProcessor()

        when: "The processor processes the IADD instruction"
        // Provide a dummy 'next' instruction with a new iid.
        processor.processInstruction(new NOP((classIndex << 35) | (methodIdx << 24) | 2))

        then: "The active frame's top operand is an IntValue representing 52"
        def resultFrame = visitor.getStack().getActiveFrame()
        def result = resultFrame.operandStack.last()
        println "Result of IADD conversion: ${result}"
        result instanceof IntValue
        result.concrete == 52
    }
}
