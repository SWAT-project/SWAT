package de.uzl.its.swat.symbolic.processor


import de.uzl.its.swat.symbolic.instruction.IADD
import de.uzl.its.swat.symbolic.instruction.INVOKEVIRTUAL
import de.uzl.its.swat.symbolic.instruction.Instruction

// Assumed conversion instruction.

import de.uzl.its.swat.symbolic.instruction.NOP
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.thread.ThreadHandler

class INVOKEVIRTUALSpec extends BaseSymbolicInstructionProcessorSpec {

    def "symbolic INVOKEVIRTUAL instruction test"() {
        given: "A stack with two integer operands and a class instance"
        String testClassName = "de.uzl.its.swat.test.TestClass"
        String testMethodName = "main"
        String testInvokeMethodName = "test"
        String testInvokeMethodDesc = "(II)I"
        def contextData = setupTestContext(testClassName, testMethodName)
        long threadId = contextData.threadId
        int classIndex = contextData.classIndex
        int methodIdx = contextData.methodIdx
        def visitor = contextData.visitor

        pushOperand(new ObjectValue(solverContext, testClassName, new IntValue(solverContext, 2)))
        IntValue i1 = pushPrimitiveOperand(42)
        IntValue i2 = pushPrimitiveOperand(10)

        and: "An INVOKEVIRTUAL instruction"
        // Create an instruction ID (iid) using classIndex, methodIdx and an arbitrary offset.
        long iid = (classIndex << 35) | (methodIdx << 24) | 1
        Instruction inst = new INVOKEVIRTUAL(iid, 0, testClassName, testInvokeMethodName, testInvokeMethodDesc)

        // Set the current instruction in the ThreadHandler.
        ThreadHandler.setCurrentInstruction(threadId, inst)

        SymbolicInstructionProcessor processor = new SymbolicInstructionProcessor()

        when: "The processor processes the INVOKEVIRTUAL instruction"
        // Provide a dummy 'next' instruction with a new iid.
        processor.processInstruction(new NOP((classIndex << 35) | (methodIdx << 24) | 2))

        then: "The a new active frame with two locals is created"
        def resultFrame = visitor.getStack().getActiveFrame()
        def frameName = resultFrame.methodName
        def ref = resultFrame.locals[0]
        def arg1 = resultFrame.locals[1]
        def arg2 = resultFrame.locals[2]
        println "Result of INVOKEVIRTUAL conversion: ${frameName} with locals ${arg1}, ${arg2}"
        frameName == testInvokeMethodName

        arg1 == i1
        arg2 == i2
    }
}
