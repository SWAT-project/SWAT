package de.uzl.its.swat.symbolic.processor

import ch.qos.logback.classic.Logger
import de.uzl.its.swat.common.logging.GlobalLogger
import de.uzl.its.swat.config.Config
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation
import de.uzl.its.swat.instrument.Intrinsics
import de.uzl.its.swat.metadata.ClassDepot
import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor
import de.uzl.its.swat.symbolic.instruction.INVOKEMETHOD_END
import de.uzl.its.swat.symbolic.instruction.INVOKESTATIC
import de.uzl.its.swat.symbolic.instruction.Instruction
import de.uzl.its.swat.symbolic.instruction.LDC_long
import de.uzl.its.swat.symbolic.instruction.NOP
import de.uzl.its.swat.symbolic.instruction.SPECIAL
import de.uzl.its.swat.symbolic.shadow.Frame
import de.uzl.its.swat.symbolic.shadow.ShadowContext
import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.BooleanObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.ByteObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.CharacterObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.DoubleObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.FloatObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.LongObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.ShortObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import de.uzl.its.swat.thread.ThreadHandler
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Specification
import de.uzl.its.swat.common.Util
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue

abstract class BaseSymbolicInstructionProcessorSpec extends Specification {
    protected final threadId = Thread.currentThread().getId()
    protected SolverContext solverContext
    protected static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    /**
     * Initializes a clean thread context, registers the test class/method in the instrumentation
     * system, and sets up an initial frame in the shadow context.
     */
    def setupTestContext(String testClassName, String testMethodName, String testDesc = "()V") {
        Config config = Config.instance()
        config.setLogShadowStateToConsole(true)
        // Clean up any existing context for this thread.
        if (ThreadHandler.hasThreadContext(threadId)) {
            ThreadHandler.removeThreadContext(threadId)
        }
        if (ThreadHandler.hasThreadContext(-1)) {
            ThreadHandler.removeThreadContext(-1)
        }
        // Create thread contexts.
        ThreadHandler.addThreadContext(-1, "Symbolic-Thread", -1)
        ThreadHandler.addThreadContext(threadId, "Test-Thread", -2)
        solverContext = ThreadHandler.getSolverContext(threadId)
        def classDepot = ClassDepot.getInstrumentationInstance()
        int classIndex = classDepot.getClassIndex(testClassName)
        GlobalStateForInstrumentation.instance.setCid(classIndex)
        ClassDepot.getInstrumentationInstance().registerTypeInfoForClass(testClassName, ["java.lang.Object"], new ArrayList<>())
        int methodIdx = classDepot.getMethodIdxForInstrumentation(testClassName, testMethodName, testDesc)
        SymbolicInstructionVisitor visitor = ThreadHandler.getSymbolicVisitor(threadId)
        ShadowContext context = visitor.getStack()
        Frame frame = new Frame(testClassName, testMethodName, methodIdx)
        context.pushFrame(frame)
        return [threadId: threadId, classIndex: classIndex, methodIdx: methodIdx, visitor: visitor]
    }


    def pushBoxedOperand(boolean value) {
        BooleanValue val = new BooleanValue(solverContext, value)
        BooleanObjectValue boxedVal = new BooleanObjectValue(solverContext, val, ObjectValue.ADDRESS_UNKNOWN)
        pushOperand(boxedVal)
        return boxedVal
    }
    def pushBoxedOperand(byte value) {
        ByteValue val = new ByteValue(solverContext, value)
        ByteObjectValue boxedVal = new ByteObjectValue(solverContext, val, ObjectValue.ADDRESS_UNKNOWN)
        pushOperand(boxedVal)
        return boxedVal
    }

    def pushBoxedOperand(char value) {
        CharValue val = new CharValue(solverContext, value)
        CharacterObjectValue boxedVal = new CharacterObjectValue(solverContext, val, ObjectValue.ADDRESS_UNKNOWN)
        pushOperand(boxedVal)
        return boxedVal
    }
    def pushBoxedOperand(int value) {
        IntValue val = new IntValue(solverContext, value)
        IntegerObjectValue boxedVal = new IntegerObjectValue(solverContext, val, ObjectValue.ADDRESS_UNKNOWN)
        pushOperand(boxedVal)
        return boxedVal
    }

    def pushBoxedOperand(long value) {
        LongValue val = new LongValue(solverContext, value)
        LongObjectValue boxedVal = new LongObjectValue(solverContext, val, ObjectValue.ADDRESS_UNKNOWN)
        pushOperand(boxedVal)
        return boxedVal
    }
    def pushBoxedOperand(float value) {
        FloatValue val = new FloatValue(solverContext, value)
        FloatObjectValue boxedVal = new FloatObjectValue(solverContext, val, ObjectValue.ADDRESS_UNKNOWN)
        pushOperand(boxedVal)
        return boxedVal
    }

    def pushBoxedOperand(double value) {
        DoubleValue val = new DoubleValue(solverContext, value)
        DoubleObjectValue boxedVal = new DoubleObjectValue(solverContext, val, ObjectValue.ADDRESS_UNKNOWN)
        pushOperand(boxedVal)
        return boxedVal
    }
    def pushBoxedOperand(short value) {
        ShortValue val = new ShortValue(solverContext, value)
        ShortObjectValue boxedVal = new ShortObjectValue(solverContext, val, ObjectValue.ADDRESS_UNKNOWN)
        pushOperand(boxedVal)
        return boxedVal
    }

    def pushStringOperand(String value) {
        StringValue val = new StringValue(solverContext, value, ObjectValue.ADDRESS_UNKNOWN)
        pushOperand(val)
        return val
    }
    // Helpers to push primitive values onto the operand stack.
    def pushPrimitiveOperand(int value) {
        IntValue val = new IntValue(solverContext, value)
        pushOperand(val)
        return val
    }

    def pushPrimitiveOperand(short value) {
        ShortValue val = new ShortValue(solverContext, value)
        pushOperand(val)
        return val
    }

    def pushPrimitiveOperand(boolean value) {
        BooleanValue val = new BooleanValue(solverContext, value)
        pushOperand(val)
        return val
    }

    def pushPrimitiveOperand(char value) {
        CharValue val = new CharValue(solverContext, value)
        pushOperand(val)
        return val
    }

    def pushPrimitiveOperand(byte value) {
        ByteValue val = new ByteValue(solverContext, value)
        pushOperand(val)
        return val
    }

    def pushPrimitiveOperand(long value) {
        LongValue val = new LongValue(solverContext, value)
        pushOperand(val, true)
        return val
    }

    def pushPrimitiveOperand(float value) {
        FloatValue val = new FloatValue(solverContext, value)
        pushOperand(val)
        return val
    }
    def pushPrimitiveOperand(double value) {
        DoubleValue val = new DoubleValue(solverContext, value)
        pushOperand(val, true)
        return val
    }

    def pushOperand(Value value, boolean wide = false) {
        SymbolicInstructionVisitor visitor = ThreadHandler.getSymbolicVisitor(threadId)
        ShadowContext context = visitor.getStack()
        if (wide) {
            context.pushWideOperand(value)
        } else {
            context.pushOperand(value)
        }
    }

    /**
     * Executes the lift instructions for a given operand and verifies the resulting symbolic value.
     *
     * @param operand The primitive value that has been pushed.
     * @param getValueCreator Closure that, given an instruction ID (iid), returns a GETVALUE instruction.
     * @param expectedType The expected class of the resulting value.
     * @param expectedConcrete The expected concrete value.
     * @param expectedSymbolicPrefix The expected symbolic prefix (e.g. IntValue.symbolicPrefix).
     * @param assignmentSignature The method signature used in injectAssignment (e.g. "(IJ)I" for int).
     * @param liftSignature The method signature used in liftValue (e.g. "(IJ)I" for int).
     * @return The resulting value (for further assertions if needed).
     */
    def executeLiftInsnSeq(long uid,
                           Closure getValueCreator,
                           Class expectedType,
                           String desc) {
        String intrinsicsClassName = Util.formatClassName(Intrinsics.class.getName())
        List<Instruction> instructions = new ArrayList<>()


        // Build the sequence of instructions.
        long iid = GlobalStateForInstrumentation.instance.incAndGetId()
        long invokeId = GlobalStateForInstrumentation.instance.incAndGetInvokeId()
        instructions.add(new INVOKESTATIC(iid, invokeId, intrinsicsClassName, "injectAssignment", desc))

        iid = GlobalStateForInstrumentation.instance.incAndGetId()
        instructions.add(new INVOKEMETHOD_END(iid, invokeId))

        iid = GlobalStateForInstrumentation.instance.incAndGetId()
        instructions.add(getValueCreator(iid))

        iid = GlobalStateForInstrumentation.instance.incAndGetId()
        instructions.add(new LDC_long(iid, uid))

        iid = GlobalStateForInstrumentation.instance.incAndGetId()
        instructions.add(new SPECIAL(iid, 0))

        iid = GlobalStateForInstrumentation.instance.incAndGetId()
        invokeId = GlobalStateForInstrumentation.instance.incAndGetInvokeId()
        instructions.add(new INVOKESTATIC(iid, invokeId, intrinsicsClassName, "liftValue", desc))

        iid = GlobalStateForInstrumentation.instance.incAndGetId()
        instructions.add(new INVOKEMETHOD_END(iid, invokeId))

        iid = GlobalStateForInstrumentation.instance.incAndGetId()
        instructions.add(new NOP(iid))

        long threadId = this.threadId

        logger.info("=" * 50)
        logger.info("Starting processing instructions.")
        logger.info("=" * 50)

        // Set the current instruction.
        ThreadHandler.setCurrentInstruction(threadId, instructions.pop())
        SymbolicInstructionProcessor processor = new SymbolicInstructionProcessor()

        while (!instructions.isEmpty()) {
            processor.processInstruction(instructions.pop())
        }
        logger.info("=" * 50)
        logger.info("Finished processing instructions.")
        logger.info("=" * 50)
        SymbolicInstructionVisitor visitor = ThreadHandler.getSymbolicVisitor(threadId)
        def resultFrame = visitor.getStack().getActiveFrame()

        def result
        if (expectedType == DoubleValue || expectedType == LongValue) {
            result = resultFrame.peek2()
        } else {
            result = resultFrame.peek()
        }
        return result
    }
}
