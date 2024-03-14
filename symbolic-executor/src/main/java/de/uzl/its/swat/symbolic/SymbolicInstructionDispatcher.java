package de.uzl.its.swat.symbolic;

import de.uzl.its.swat.symbolic.instruction.LOOP_BEGIN;
import de.uzl.its.swat.symbolic.instruction.LOOP_END;
import de.uzl.its.swat.symbolic.instruction.MAKE_SYMBOLIC;
import de.uzl.its.swat.symbolic.processor.InstructionProcessor;
import de.uzl.its.swat.thread.ThreadHandler;

public final class SymbolicInstructionDispatcher {

    private SymbolicInstructionDispatcher() {}

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_int instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_int}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_int(LDC_int)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, int c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_int instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_int}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_int(LDC_int)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param c The constant to load
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, int mid, int c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_long instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_long}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_long(LDC_long)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load.
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, long c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_long instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_long}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_long(LDC_long)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param c The constant to load.
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, int mid, long c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_float instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_float}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_float(LDC_float)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load.
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, float c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_float instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_float}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_float(LDC_float)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param c The constant to load
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, int mid, float c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_double instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_double}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_double(LDC_double)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load.
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, double c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_double instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_double}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_double(LDC_double)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param c The constant to load.
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, int mid, double c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_String instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_String}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_String(LDC_String)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, String c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_String instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_String}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_String(LDC_String)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param c the address of the object that's loaded
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, int mid, String c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_Object instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_Object}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_Object(LDC_Object)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c the address of the object that's loaded
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, Object c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_Object instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_Object}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDC_Object(LDC_Object)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param c the address of the object that's loaded
     */
    @SuppressWarnings("unused")
    public static void LDC(int iid, int mid, Object c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IINC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IINC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIINC(IINC)}
     *
     * @param var the local variable index.
     * @param increment the increment amount.
     */
    @SuppressWarnings("unused")
    public static void IINC(int var, int increment) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IINC(-1, -1, var, increment);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IINC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IINC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIINC(IINC)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var the local variable index.
     * @param increment the increment amount.
     */
    @SuppressWarnings("unused")
    public static void IINC(int iid, int mid, int var, int increment) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IINC(iid, mid, var, increment);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MULTIANEWARRAY instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MULTIANEWARRAY}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitMULTIANEWARRAY(MULTIANEWARRAY)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param desc the descriptor of the array
     * @param dims the number of dimensions
     */
    @SuppressWarnings("unused")
    public static void MULTIANEWARRAY(int iid, String desc, int dims) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MULTIANEWARRAY(iid, -1, desc, dims);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MULTIANEWARRAY instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MULTIANEWARRAY}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitMULTIANEWARRAY(MULTIANEWARRAY)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param desc the descriptor of the array
     * @param dims the number of dimensions
     */
    @SuppressWarnings("unused")
    public static void MULTIANEWARRAY(int iid, int mid, String desc, int dims) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MULTIANEWARRAY(iid, mid, desc, dims);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LOOKUPSWITCH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LOOKUPSWITCH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLOOKUPSWITCH(LOOKUPSWITCH)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param dflt beginning of the default handler block.
     * @param keys the values of the keys.
     * @param labels beginnings of the handler blocks.
     */
    @SuppressWarnings("unused")
    public static void LOOKUPSWITCH(int iid, int dflt, int[] keys, int[] labels) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOOKUPSWITCH(iid, -1, dflt, keys, labels);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LOOKUPSWITCH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LOOKUPSWITCH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLOOKUPSWITCH(LOOKUPSWITCH)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param dflt beginning of the default handler block.
     * @param keys the values of the keys.
     * @param labels beginnings of the handler blocks.
     */
    @SuppressWarnings("unused")
    public static void LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOOKUPSWITCH(iid, mid, dflt, keys, labels);
    }
    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the TABLESWITCH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.TABLESWITCH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitTABLESWITCH(TABLESWITCH)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param min min value.
     * @param max max value.
     * @param dflt beginning of the default handler block.
     * @param labels beginnings of the handler blocks.
     * @param values the conditions.
     */
    @SuppressWarnings("unused")
    public static void TABLESWITCH(
            int iid, int min, int max, int dflt, int[] labels, int[] values) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.TABLESWITCH(iid, -1, min, max, dflt, labels, values);
    }
    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the TABLESWITCH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.TABLESWITCH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitTABLESWITCH(TABLESWITCH)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param min min value.
     * @param max max value.
     * @param dflt beginning of the default handler block.
     * @param labels beginnings of the handler blocks.
     * @param values the conditions.
     */
    @SuppressWarnings("unused")
    public static void TABLESWITCH(
            int iid, int mid, int min, int max, int dflt, int[] labels, int[] values) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.TABLESWITCH(iid, -1, min, max, dflt, labels, values);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFEQ instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFEQ}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFEQ(IFEQ)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFEQ(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFEQ(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFEQ instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFEQ}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFEQ(IFEQ)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFEQ(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFEQ(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFNE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFNE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFNE(IFNE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFNE(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNE(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFNE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFNE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFNE(IFNE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFNE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNE(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFLT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFLT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFLT(IFLT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFLT(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLT(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFLT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFLT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFLT(IFLT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFLT(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLT(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFGE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFGE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFGE(IFGE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFGE(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGE(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFGE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFGE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFGE(IFGE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFGE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGE(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFGT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFGT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFGT(IFGT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFGT(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGT(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFGT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFGT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFGT(IFGT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFGT(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGT(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFLE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFLE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFLE(IFLE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFLE(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLE(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFLE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFLE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFLE(IFLE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFLE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLE(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPEQ instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPEQ}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPEQ(IF_ICMPEQ)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPEQ(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPEQ(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPEQ instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPEQ}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPEQ(IF_ICMPEQ)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPEQ(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPEQ(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPNE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPNE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPNE(IF_ICMPNE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPNE(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPNE(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPNE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPNE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPNE(IF_ICMPNE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPNE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPNE(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPLT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPLT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPLT(IF_ICMPLT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPLT(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLT(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPLT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPLT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPLT(IF_ICMPLT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPLT(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLT(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPGE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPGE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPGE(IF_ICMPGE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPGE(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGE(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPGE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPGE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPGE(IF_ICMPGE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPGE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGE(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPGT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPGT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPGT(IF_ICMPGT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPGT(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGT(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPGT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPGT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPGT(IF_ICMPGT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPGT(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGT(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPLE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPLE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPLE(IF_ICMPLE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPLE(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLE(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPLE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPLE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ICMPLE(IF_ICMPLE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPLE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLE(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ACMPEQ instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ACMPEQ}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ACMPEQ(IF_ACMPEQ)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ACMPEQ(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPEQ(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ACMPEQ instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ACMPEQ}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ACMPEQ(IF_ACMPEQ)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ACMPEQ(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPEQ(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ACMPNE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ACMPNE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ACMPNE(IF_ACMPNE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ACMPNE(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPNE(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ACMPNE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ACMPNE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIF_ACMPNE(IF_ACMPNE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IF_ACMPNE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPNE(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GOTO instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GOTO}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGOTO(GOTO)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void GOTO(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GOTO(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GOTO instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GOTO}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGOTO(GOTO)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void GOTO(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GOTO(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the JSR instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.JSR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitJSR(JSR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void JSR(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.JSR(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the JSR instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.JSR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitJSR(JSR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void JSR(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.JSR(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFNULL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFNULL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFNULL(IFNULL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFNULL(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNULL(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFNULL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFNULL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFNULL(IFNULL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFNULL(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNULL(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFNONNULL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFNONNULL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFNONNULL(IFNONNULL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFNONNULL(int iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNONNULL(iid, -1, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFNONNULL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFNONNULL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIFNONNULL(IFNONNULL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFNONNULL(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNONNULL(iid, mid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEVIRTUAL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEVIRTUAL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKEVIRTUAL(INVOKEVIRTUAL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKEVIRTUAL(int iid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEVIRTUAL(iid, -1, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEVIRTUAL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEVIRTUAL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKEVIRTUAL(INVOKEVIRTUAL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKEVIRTUAL(int iid, int mid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEVIRTUAL(iid, mid, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKESPECIAL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKESPECIAL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKESPECIAL(INVOKESPECIAL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKESPECIAL(int iid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESPECIAL(iid, -1, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKESPECIAL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKESPECIAL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKESPECIAL(INVOKESPECIAL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKESPECIAL(int iid, int mid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESPECIAL(iid, mid, owner, name, desc);
    }
    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEDYNAMIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEDYNAMIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKEDYNAMIC(INVOKEDYNAMIC)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKEDYNAMIC(
            int iid, String owner, String name, String desc, String lambda) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEDYNAMIC(iid, -1, owner, name, desc, lambda);
    }
    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEDYNAMIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEDYNAMIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKEDYNAMIC(INVOKEDYNAMIC)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKEDYNAMIC(
            int iid, int mid, String owner, String name, String desc, String lambda) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEDYNAMIC(iid, mid, owner, name, desc, lambda);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKESTATIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKESTATIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKESTATIC(INVOKESTATIC)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKESTATIC(int iid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESTATIC(iid, -1, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKESTATIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKESTATIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKESTATIC(INVOKESTATIC)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKESTATIC(int iid, int mid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESTATIC(iid, mid, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEINTERFACE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEINTERFACE}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKEINTERFACE(INVOKEINTERFACE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKEINTERFACE(int iid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEINTERFACE(iid, -1, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEINTERFACE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEINTERFACE}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKEINTERFACE(INVOKEINTERFACE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKEINTERFACE(int iid, int mid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEINTERFACE(iid, mid, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETSTATIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETSTATIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETSTATIC(GETSTATIC)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void GETSTATIC(int iid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETSTATIC(iid, -1, cIdx, fIdx, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETSTATIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETSTATIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETSTATIC(GETSTATIC)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETSTATIC(iid, mid, cIdx, fIdx, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the PUTSTATIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.PUTSTATIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitPUTSTATIC(PUTSTATIC)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void PUTSTATIC(int iid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTSTATIC(iid, -1, cIdx, fIdx, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the PUTSTATIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.PUTSTATIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitPUTSTATIC(PUTSTATIC)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTSTATIC(iid, mid, cIdx, fIdx, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETFIELD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETFIELD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETFIELD(GETFIELD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void GETFIELD(int iid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETFIELD(iid, -1, cIdx, fIdx, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETFIELD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETFIELD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETFIELD(GETFIELD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETFIELD(iid, mid, cIdx, fIdx, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the PUTFIELD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.PUTFIELD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitPUTFIELD(PUTFIELD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void PUTFIELD(int iid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTFIELD(iid, -1, cIdx, fIdx, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the PUTFIELD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.PUTFIELD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitPUTFIELD(PUTFIELD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTFIELD(iid, mid, cIdx, fIdx, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the NEW instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.NEW}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitNEW(NEW)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param type the type of the object to be created
     * @param cIdx the class index
     */
    @SuppressWarnings("unused")
    public static void NEW(int iid, String type, int cIdx) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEW(iid, -1, type, cIdx);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the NEW instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.NEW}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitNEW(NEW)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param type the type of the object to be created
     * @param cIdx the class index
     */
    @SuppressWarnings("unused")
    public static void NEW(int iid, int mid, String type, int cIdx) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEW(iid, mid, type, cIdx);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ANEWARRAY instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ANEWARRAY}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitANEWARRAY(ANEWARRAY)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param type type of the array to create.
     */
    @SuppressWarnings("unused")
    public static void ANEWARRAY(int iid, String type) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ANEWARRAY(iid, -1, type);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ANEWARRAY instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ANEWARRAY}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitANEWARRAY(ANEWARRAY)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param type type of the array to create.
     */
    @SuppressWarnings("unused")
    public static void ANEWARRAY(int iid, int mid, String type) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ANEWARRAY(iid, mid, type);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CHECKCAST instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CHECKCAST}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitCHECKCAST(CHECKCAST)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param type the type to check against
     */
    @SuppressWarnings("unused")
    public static void CHECKCAST(int iid, String type) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CHECKCAST(iid, -1, type);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CHECKCAST instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CHECKCAST}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitCHECKCAST(CHECKCAST)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param type the type to check against
     */
    @SuppressWarnings("unused")
    public static void CHECKCAST(int iid, int mid, String type) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CHECKCAST(iid, mid, type);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INSTANCEOF instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INSTANCEOF}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINSTANCEOF(INSTANCEOF)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param type the type to check against
     */
    @SuppressWarnings("unused")
    public static void INSTANCEOF(int iid, String type) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INSTANCEOF(iid, -1, type);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INSTANCEOF instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INSTANCEOF}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINSTANCEOF(INSTANCEOF)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param type the type to check against
     */
    @SuppressWarnings("unused")
    public static void INSTANCEOF(int iid, int mid, String type) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INSTANCEOF(iid, mid, type);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the BIPUSH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.BIPUSH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitBIPUSH(BIPUSH)}
     *
     * @param value the value to be pushed.
     */
    @SuppressWarnings("unused")
    public static void BIPUSH(int value) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BIPUSH(-1, -1, value);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the BIPUSH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.BIPUSH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitBIPUSH(BIPUSH)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param value the value to be pushed.
     */
    @SuppressWarnings("unused")
    public static void BIPUSH(int iid, int mid, int value) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BIPUSH(iid, mid, value);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SIPUSH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SIPUSH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitSIPUSH(SIPUSH)}
     *
     * @param value the value to be pushed.
     */
    @SuppressWarnings("unused")
    public static void SIPUSH(int value) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SIPUSH(-1, -1, value);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SIPUSH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SIPUSH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitSIPUSH(SIPUSH)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param value the value to be pushed.
     */
    @SuppressWarnings("unused")
    public static void SIPUSH(int iid, int mid, int value) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SIPUSH(iid, mid, value);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the NEWARRAY instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.NEWARRAY}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitNEWARRAY(NEWARRAY)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param atype the type of the array to be created
     */
    @SuppressWarnings("unused")
    public static void NEWARRAY(int iid, int atype) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEWARRAY(iid, -1, atype);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the NEWARRAY instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.NEWARRAY}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitNEWARRAY(NEWARRAY)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param atype the type of the array to be created
     */
    @SuppressWarnings("unused")
    public static void NEWARRAY(int iid, int mid, int atype) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEWARRAY(iid, mid, atype);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ILOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ILOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitILOAD(ILOAD)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ILOAD(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ILOAD(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ILOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ILOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitILOAD(ILOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ILOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ILOAD(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LLOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LLOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLLOAD(LLOAD)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void LLOAD(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LLOAD(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LLOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LLOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLLOAD(LLOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void LLOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LLOAD(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FLOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FLOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFLOAD(FLOAD)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void FLOAD(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FLOAD(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FLOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FLOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFLOAD(FLOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void FLOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FLOAD(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DLOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DLOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDLOAD(DLOAD)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void DLOAD(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DLOAD(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DLOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DLOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDLOAD(DLOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void DLOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DLOAD(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitALOAD(ALOAD)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ALOAD(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ALOAD(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitALOAD(ALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ALOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ALOAD(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitISTORE(ISTORE)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ISTORE(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISTORE(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitISTORE(ISTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ISTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISTORE(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLSTORE(LSTORE)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void LSTORE(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSTORE(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLSTORE(LSTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void LSTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSTORE(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FSTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FSTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFSTORE(FSTORE)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void FSTORE(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSTORE(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FSTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FSTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFSTORE(FSTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void FSTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSTORE(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DSTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DSTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDSTORE(DSTORE)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void DSTORE(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSTORE(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DSTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DSTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDSTORE(DSTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void DSTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSTORE(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitASTORE(ASTORE)}
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ASTORE(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ASTORE(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitASTORE(ASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ASTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ASTORE(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the RET instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.RET}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitRET(RET)}
     *
     * @param var local variable index to the return address
     */
    @SuppressWarnings("unused")
    public static void RET(int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RET(-1, -1, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the RET instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.RET}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitRET(RET)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     * @param var local variable index to the return address
     */
    @SuppressWarnings("unused")
    public static void RET(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RET(iid, mid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the NOP instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.NOP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitNOP(NOP)}
     */
    @SuppressWarnings("unused")
    public static void NOP() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NOP(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the NOP instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.NOP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitNOP(NOP)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void NOP(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NOP(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ACONST_NULL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ACONST_NULL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitACONST_NULL(ACONST_NULL)}
     */
    @SuppressWarnings("unused")
    public static void ACONST_NULL() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ACONST_NULL(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ACONST_NULL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ACONST_NULL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitACONST_NULL(ACONST_NULL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ACONST_NULL(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ACONST_NULL(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_M1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_M1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_M1(ICONST_M1)}
     */
    @SuppressWarnings("unused")
    public static void ICONST_M1() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_M1(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_M1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_M1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_M1(ICONST_M1)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ICONST_M1(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_M1(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_0(ICONST_0)}
     */
    @SuppressWarnings("unused")
    public static void ICONST_0() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_0(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_0(ICONST_0)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ICONST_0(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_0(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_1(ICONST_1)}
     */
    @SuppressWarnings("unused")
    public static void ICONST_1() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_1(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_1(ICONST_1)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ICONST_1(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_1(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_2(ICONST_2)}
     */
    @SuppressWarnings("unused")
    public static void ICONST_2() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_2(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_2(ICONST_2)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ICONST_2(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_2(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_3 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_3}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_3(ICONST_3)}
     */
    @SuppressWarnings("unused")
    public static void ICONST_3() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_3(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_3 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_3}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_3(ICONST_3)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ICONST_3(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_3(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_4 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_4}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_4(ICONST_4)}
     */
    @SuppressWarnings("unused")
    public static void ICONST_4() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_4(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_4 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_4}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_4(ICONST_4)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ICONST_4(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_4(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_5 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_5}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_5(ICONST_5)}
     */
    @SuppressWarnings("unused")
    public static void ICONST_5() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_5(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_5 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_5}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitICONST_5(ICONST_5)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ICONST_5(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_5(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LCONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LCONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLCONST_0(LCONST_0)}
     */
    @SuppressWarnings("unused")
    public static void LCONST_0() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_0(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LCONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LCONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLCONST_0(LCONST_0)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LCONST_0(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_0(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LCONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LCONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLCONST_1(LCONST_1)}
     */
    @SuppressWarnings("unused")
    public static void LCONST_1() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_1(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LCONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LCONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLCONST_1(LCONST_1)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LCONST_1(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_1(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCONST_0(FCONST_0)}
     */
    @SuppressWarnings("unused")
    public static void FCONST_0() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_0(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCONST_0(FCONST_0)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FCONST_0(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_0(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCONST_1(FCONST_1)}
     */
    @SuppressWarnings("unused")
    public static void FCONST_1() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_1(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCONST_1(FCONST_1)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FCONST_1(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_1(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCONST_2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCONST_2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCONST_2(FCONST_2)}
     */
    @SuppressWarnings("unused")
    public static void FCONST_2() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_2(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCONST_2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCONST_2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCONST_2(FCONST_2)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FCONST_2(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_2(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDCONST_0(DCONST_0)}
     */
    @SuppressWarnings("unused")
    public static void DCONST_0() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_0(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDCONST_0(DCONST_0)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DCONST_0(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_0(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDCONST_1(DCONST_1)}
     */
    @SuppressWarnings("unused")
    public static void DCONST_1() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_1(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDCONST_1(DCONST_1)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DCONST_1(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_1(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIALOAD(IALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IALOAD(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IALOAD(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIALOAD(IALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IALOAD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLALOAD(LALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LALOAD(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LALOAD(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLALOAD(LALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LALOAD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFALOAD(FALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FALOAD(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FALOAD(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFALOAD(FALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FALOAD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDALOAD(DALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DALOAD(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DALOAD(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDALOAD(DALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DALOAD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the AALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.AALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitAALOAD(AALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void AALOAD(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AALOAD(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the AALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.AALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitAALOAD(AALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void AALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AALOAD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the BALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.BALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitBALOAD(BALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void BALOAD(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BALOAD(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the BALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.BALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitBALOAD(BALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void BALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BALOAD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitCALOAD(CALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void CALOAD(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CALOAD(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitCALOAD(CALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void CALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CALOAD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitSALOAD(SALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void SALOAD(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SALOAD(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitSALOAD(SALOAD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void SALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SALOAD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIASTORE(IASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IASTORE(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IASTORE(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIASTORE(IASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IASTORE(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLASTORE(LASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LASTORE(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LASTORE(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLASTORE(LASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LASTORE(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFASTORE(FASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FASTORE(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FASTORE(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFASTORE(FASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FASTORE(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDASTORE(DASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DASTORE(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DASTORE(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDASTORE(DASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DASTORE(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the AASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.AASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitAASTORE(AASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void AASTORE(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AASTORE(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the AASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.AASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitAASTORE(AASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void AASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AASTORE(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the BASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.BASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitBASTORE(BASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void BASTORE(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BASTORE(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the BASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.BASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitBASTORE(BASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void BASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BASTORE(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitCASTORE(CASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void CASTORE(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CASTORE(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitCASTORE(CASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void CASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CASTORE(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitSASTORE(SASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void SASTORE(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SASTORE(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitSASTORE(SASTORE)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void SASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SASTORE(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the POP instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.POP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitPOP(POP)}
     */
    @SuppressWarnings("unused")
    public static void POP() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the POP instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.POP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitPOP(POP)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void POP(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the POP2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.POP2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitPOP2(POP2)}
     */
    @SuppressWarnings("unused")
    public static void POP2() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP2(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the POP2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.POP2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitPOP2(POP2)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void POP2(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP2(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP(DUP)}
     */
    @SuppressWarnings("unused")
    public static void DUP() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP(DUP)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DUP(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP_X1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP_X1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP_X1(DUP_X1)}
     */
    @SuppressWarnings("unused")
    public static void DUP_X1() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X1(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP_X1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP_X1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP_X1(DUP_X1)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DUP_X1(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X1(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP_X2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP_X2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP_X2(DUP_X2)}
     */
    @SuppressWarnings("unused")
    public static void DUP_X2() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X2(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP_X2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP_X2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP_X2(DUP_X2)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DUP_X2(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X2(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP2(DUP2)}
     */
    @SuppressWarnings("unused")
    public static void DUP2() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP2(DUP2)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DUP2(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP2_X1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP2_X1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP2_X1(DUP2_X1)}
     */
    @SuppressWarnings("unused")
    public static void DUP2_X1() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X1(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP2_X1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP2_X1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP2_X1(DUP2_X1)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DUP2_X1(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X1(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP2_X2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP2_X2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP2_X2(DUP2_X2)}
     */
    @SuppressWarnings("unused")
    public static void DUP2_X2() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X2(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP2_X2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP2_X2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDUP2_X2(DUP2_X2)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DUP2_X2(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X2(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SWAP instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SWAP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitSWAP(SWAP)}
     */
    @SuppressWarnings("unused")
    public static void SWAP() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SWAP(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SWAP instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SWAP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitSWAP(SWAP)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void SWAP(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SWAP(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIADD(IADD)}
     */
    @SuppressWarnings("unused")
    public static void IADD() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IADD(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIADD(IADD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IADD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IADD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLADD(LADD)}
     */
    @SuppressWarnings("unused")
    public static void LADD() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LADD(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLADD(LADD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LADD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LADD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFADD(FADD)}
     */
    @SuppressWarnings("unused")
    public static void FADD() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FADD(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFADD(FADD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FADD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FADD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDADD(DADD)}
     */
    @SuppressWarnings("unused")
    public static void DADD() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DADD(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDADD(DADD)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DADD(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DADD(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitISUB(ISUB)}
     */
    @SuppressWarnings("unused")
    public static void ISUB() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISUB(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitISUB(ISUB)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ISUB(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISUB(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLSUB(LSUB)}
     */
    @SuppressWarnings("unused")
    public static void LSUB() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSUB(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLSUB(LSUB)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LSUB(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSUB(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FSUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FSUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFSUB(FSUB)}
     */
    @SuppressWarnings("unused")
    public static void FSUB() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSUB(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FSUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FSUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFSUB(FSUB)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FSUB(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSUB(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DSUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DSUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDSUB(DSUB)}
     */
    @SuppressWarnings("unused")
    public static void DSUB() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSUB(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DSUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DSUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDSUB(DSUB)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DSUB(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSUB(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIMUL(IMUL)}
     */
    @SuppressWarnings("unused")
    public static void IMUL() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IMUL(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIMUL(IMUL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IMUL(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IMUL(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLMUL(LMUL)}
     */
    @SuppressWarnings("unused")
    public static void LMUL() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LMUL(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLMUL(LMUL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LMUL(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LMUL(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFMUL(FMUL)}
     */
    @SuppressWarnings("unused")
    public static void FMUL() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FMUL(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFMUL(FMUL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FMUL(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FMUL(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDMUL(DMUL)}
     */
    @SuppressWarnings("unused")
    public static void DMUL() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DMUL(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDMUL(DMUL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DMUL(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DMUL(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIDIV(IDIV)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IDIV(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IDIV(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIDIV(IDIV)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IDIV(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IDIV(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDIV(LDIV)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LDIV(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDIV(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLDIV(LDIV)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LDIV(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDIV(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFDIV(FDIV)}
     */
    @SuppressWarnings("unused")
    public static void FDIV() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FDIV(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFDIV(FDIV)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FDIV(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FDIV(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDDIV(DDIV)}
     */
    @SuppressWarnings("unused")
    public static void DDIV() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DDIV(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDDIV(DDIV)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DDIV(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DDIV(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIREM(IREM)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IREM(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IREM(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIREM(IREM)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IREM(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IREM(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLREM(LREM)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LREM(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LREM(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLREM(LREM)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LREM(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LREM(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFREM(FREM)}
     */
    @SuppressWarnings("unused")
    public static void FREM() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FREM(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFREM(FREM)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FREM(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FREM(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDREM(DREM)}
     */
    @SuppressWarnings("unused")
    public static void DREM() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DREM(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDREM(DREM)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DREM(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DREM(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINEG(INEG)}
     */
    @SuppressWarnings("unused")
    public static void INEG() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INEG(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINEG(INEG)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void INEG(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INEG(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LNEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LNEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLNEG(LNEG)}
     */
    @SuppressWarnings("unused")
    public static void LNEG() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LNEG(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LNEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LNEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLNEG(LNEG)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LNEG(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LNEG(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FNEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FNEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFNEG(FNEG)}
     */
    @SuppressWarnings("unused")
    public static void FNEG() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FNEG(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FNEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FNEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFNEG(FNEG)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FNEG(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FNEG(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DNEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DNEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDNEG(DNEG)}
     */
    @SuppressWarnings("unused")
    public static void DNEG() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DNEG(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DNEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DNEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDNEG(DNEG)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DNEG(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DNEG(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISHL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISHL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitISHL(ISHL)}
     */
    @SuppressWarnings("unused")
    public static void ISHL() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHL(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISHL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISHL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitISHL(ISHL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ISHL(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHL(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSHL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSHL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLSHL(LSHL)}
     */
    @SuppressWarnings("unused")
    public static void LSHL() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHL(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSHL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSHL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLSHL(LSHL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LSHL(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHL(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitISHR(ISHR)}
     */
    @SuppressWarnings("unused")
    public static void ISHR() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHR(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitISHR(ISHR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ISHR(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHR(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLSHR(LSHR)}
     */
    @SuppressWarnings("unused")
    public static void LSHR() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHR(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLSHR(LSHR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LSHR(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHR(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IUSHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IUSHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIUSHR(IUSHR)}
     */
    @SuppressWarnings("unused")
    public static void IUSHR() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IUSHR(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IUSHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IUSHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIUSHR(IUSHR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IUSHR(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IUSHR(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LUSHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LUSHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLUSHR(LUSHR)}
     */
    @SuppressWarnings("unused")
    public static void LUSHR() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LUSHR(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LUSHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LUSHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLUSHR(LUSHR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LUSHR(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LUSHR(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IAND instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IAND}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIAND(IAND)}
     */
    @SuppressWarnings("unused")
    public static void IAND() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IAND(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IAND instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IAND}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIAND(IAND)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IAND(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IAND(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LAND instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LAND}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLAND(LAND)}
     */
    @SuppressWarnings("unused")
    public static void LAND() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LAND(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LAND instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LAND}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLAND(LAND)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LAND(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LAND(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IOR instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIOR(IOR)}
     */
    @SuppressWarnings("unused")
    public static void IOR() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IOR(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IOR instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIOR(IOR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IOR(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IOR(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LOR instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLOR(LOR)}
     */
    @SuppressWarnings("unused")
    public static void LOR() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOR(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LOR instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLOR(LOR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LOR(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOR(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IXOR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IXOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIXOR(IXOR)}
     */
    @SuppressWarnings("unused")
    public static void IXOR() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IXOR(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IXOR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IXOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIXOR(IXOR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IXOR(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IXOR(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LXOR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LXOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLXOR(LXOR)}
     */
    @SuppressWarnings("unused")
    public static void LXOR() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LXOR(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LXOR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LXOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLXOR(LXOR)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LXOR(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LXOR(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2L instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2L}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2L(I2L)}
     */
    @SuppressWarnings("unused")
    public static void I2L() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2L(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2L instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2L}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2L(I2L)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void I2L(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2L(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2F instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2F}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2F(I2F)}
     */
    @SuppressWarnings("unused")
    public static void I2F() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2F(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2F instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2F}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2F(I2F)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void I2F(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2F(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2D instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2D}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2D(I2D)}
     */
    @SuppressWarnings("unused")
    public static void I2D() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2D(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2D instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2D}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2D(I2D)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void I2D(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2D(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the L2I instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.L2I}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitL2I(L2I)}
     */
    @SuppressWarnings("unused")
    public static void L2I() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2I(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the L2I instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.L2I}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitL2I(L2I)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void L2I(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2I(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the L2F instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.L2F}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitL2F(L2F)}
     */
    @SuppressWarnings("unused")
    public static void L2F() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2F(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the L2F instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.L2F}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitL2F(L2F)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void L2F(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2F(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the L2D instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.L2D}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitL2D(L2D)}
     */
    @SuppressWarnings("unused")
    public static void L2D() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2D(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the L2D instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.L2D}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitL2D(L2D)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void L2D(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2D(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the F2I instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.F2I}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitF2I(F2I)}
     */
    @SuppressWarnings("unused")
    public static void F2I() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2I(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the F2I instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.F2I}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitF2I(F2I)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void F2I(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2I(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the F2L instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.F2L}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitF2L(F2L)}
     */
    @SuppressWarnings("unused")
    public static void F2L() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2L(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the F2L instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.F2L}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitF2L(F2L)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void F2L(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2L(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the F2D instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.F2D}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitF2D(F2D)}
     */
    @SuppressWarnings("unused")
    public static void F2D() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2D(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the F2D instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.F2D}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitF2D(F2D)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void F2D(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2D(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the D2I instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.D2I}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitD2I(D2I)}
     */
    @SuppressWarnings("unused")
    public static void D2I() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2I(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the D2I instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.D2I}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitD2I(D2I)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void D2I(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2I(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the D2L instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.D2L}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitD2L(D2L)}
     */
    @SuppressWarnings("unused")
    public static void D2L() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2L(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the D2L instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.D2L}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitD2L(D2L)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void D2L(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2L(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the D2F instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.D2F}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitD2F(D2F)}
     */
    @SuppressWarnings("unused")
    public static void D2F() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2F(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the D2F instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.D2F}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitD2F(D2F)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void D2F(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2F(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2B instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2B}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2B(I2B)}
     */
    @SuppressWarnings("unused")
    public static void I2B() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2B(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2B instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2B}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2B(I2B)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void I2B(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2B(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2C instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2C}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2C(I2C)}
     */
    @SuppressWarnings("unused")
    public static void I2C() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2C(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2C instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2C}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2C(I2C)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void I2C(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2C(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2S instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2S}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2S(I2S)}
     */
    @SuppressWarnings("unused")
    public static void I2S() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2S(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2S instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2S}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitI2S(I2S)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void I2S(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2S(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LCMP instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LCMP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLCMP(LCMP)}
     */
    @SuppressWarnings("unused")
    public static void LCMP() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCMP(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LCMP instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LCMP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLCMP(LCMP)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LCMP(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCMP(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCMPL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCMPL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCMPL(FCMPL)}
     */
    @SuppressWarnings("unused")
    public static void FCMPL() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPL(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCMPL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCMPL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCMPL(FCMPL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FCMPL(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPL(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCMPG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCMPG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCMPG(FCMPG)}
     */
    @SuppressWarnings("unused")
    public static void FCMPG() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPG(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCMPG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCMPG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFCMPG(FCMPG)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FCMPG(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPG(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCMPL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCMPL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDCMPL(DCMPL)}
     */
    @SuppressWarnings("unused")
    public static void DCMPL() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPL(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCMPL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCMPL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDCMPL(DCMPL)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DCMPL(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPL(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCMPG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCMPG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDCMPG(DCMPG)}
     */
    @SuppressWarnings("unused")
    public static void DCMPG() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPG(-1, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCMPG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCMPG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDCMPG(DCMPG)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DCMPG(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPG(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIRETURN(IRETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IRETURN(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IRETURN(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitIRETURN(IRETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void IRETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IRETURN(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLRETURN(LRETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LRETURN(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LRETURN(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLRETURN(LRETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void LRETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LRETURN(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFRETURN(FRETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FRETURN(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FRETURN(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitFRETURN(FRETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void FRETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FRETURN(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDRETURN(DRETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DRETURN(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DRETURN(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitDRETURN(DRETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void DRETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DRETURN(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ARETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ARETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitARETURN(ARETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ARETURN(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARETURN(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ARETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ARETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitARETURN(ARETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ARETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARETURN(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the RETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.RETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitRETURN(RETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void RETURN(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RETURN(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the RETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.RETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitRETURN(RETURN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void RETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RETURN(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ARRAYLENGTH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ARRAYLENGTH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitARRAYLENGTH(ARRAYLENGTH)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ARRAYLENGTH(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARRAYLENGTH(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ARRAYLENGTH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ARRAYLENGTH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitARRAYLENGTH(ARRAYLENGTH)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ARRAYLENGTH(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARRAYLENGTH(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ATHROW instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ATHROW}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitATHROW(ATHROW)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ATHROW(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ATHROW(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ATHROW instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ATHROW}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitATHROW(ATHROW)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void ATHROW(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ATHROW(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MONITORENTER instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MONITORENTER}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitMONITORENTER(MONITORENTER)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void MONITORENTER(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITORENTER(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MONITORENTER instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MONITORENTER}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitMONITORENTER(MONITORENTER)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void MONITORENTER(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITORENTER(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MONITOREXIT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MONITOREXIT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitMONITOREXIT(MONITOREXIT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void MONITOREXIT(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITOREXIT(iid, -1);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MONITOREXIT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MONITOREXIT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitMONITOREXIT(MONITOREXIT)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param mid The unique method ID set during instrumentation. If not used, mid is -1.
     */
    @SuppressWarnings("unused")
    public static void MONITOREXIT(int iid, int mid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITOREXIT(iid, mid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_double instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_double}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_double(GETVALUE_double)}
     *
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_double(double v, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_double(v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_long instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_long}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_long(GETVALUE_long)}
     *
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_long(long v, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_long(v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_Object instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_Object}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_Object(GETVALUE_Object)}
     *
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_Object(Object v, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_Object(v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_boolean instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_boolean}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_boolean(GETVALUE_boolean)}
     *
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_boolean(boolean v, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_boolean(v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_byte instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_byte}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_byte(GETVALUE_byte)}
     *
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_byte(byte v, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_byte(v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_char instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_char}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_char(GETVALUE_char)}
     *
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_char(char v, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_char(v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_float instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_float}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_float(GETVALUE_float)}
     *
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_float(float v, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_float(v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_int instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_int}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_int(GETVALUE_int)}
     *
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_int(int v, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_int(v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_short instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_short}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_short(GETVALUE_short)}
     *
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_short(short v, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_short(v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_void instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_void}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitGETVALUE_void(GETVALUE_void)}
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_void() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_void();
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEMETHOD_EXCEPTION instruction is executed
     * in an instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEMETHOD_EXCEPTION}. The instruction is handled in
     * the {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKEMETHOD_EXCEPTION(INVOKEMETHOD_EXCEPTION)}
     */
    @SuppressWarnings("unused")
    public static void INVOKEMETHOD_EXCEPTION() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEMETHOD_EXCEPTION();
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEMETHOD_END instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEMETHOD_END}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitINVOKEMETHOD_END(INVOKEMETHOD_END)}
     */
    @SuppressWarnings("unused")
    public static void INVOKEMETHOD_END() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEMETHOD_END();
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SPECIAL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SPECIAL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitSPECIAL(SPECIAL)}
     *
     * @param i marker used in symbolic interpreter for example for branch detection.
     */
    @SuppressWarnings("unused")
    public static void SPECIAL(int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SPECIAL(i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MAKE_SYMBOLIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MAKE_SYMBOLIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitMAKE_SYMBOLIC(MAKE_SYMBOLIC)}
     */
    @SuppressWarnings("unused")
    public static void MAKE_SYMBOLIC() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MAKE_SYMBOLIC();
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LOOP_BEGIN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LOOP_BEGIN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLOOP_BEGIN(LOOP_BEGIN)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LOOP_BEGIN(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOOP_BEGIN(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LOOP_END instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LOOP_END}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method: {@link
     * SymbolicInstructionVisitor#visitLOOP_END(LOOP_END)}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LOOP_END(int iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOOP_END(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the flush instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the visitor method in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor}.
     */
    @SuppressWarnings("unused")
    public static void flush() {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.flush();
    }
}
