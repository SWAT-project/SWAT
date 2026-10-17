package de.uzl.its.swat.symbolic.dispatcher;

import de.uzl.its.swat.symbolic.SymbolicInstructionVisitor;
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
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load
     */
    @SuppressWarnings("unused")
    public static void LDC(long iid, int c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_long instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_long}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load.
     */
    @SuppressWarnings("unused")
    public static void LDC(long iid, long c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_float instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_float}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load.
     */
    @SuppressWarnings("unused")
    public static void LDC(long iid, float c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_double instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_double}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load.
     */
    @SuppressWarnings("unused")
    public static void LDC(long iid, double c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_String instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_String}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c The constant to load
     */
    @SuppressWarnings("unused")
    public static void LDC(long iid, String c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDC_Object instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDC_Object}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param c the address of the object that's loaded
     */
    @SuppressWarnings("unused")
    public static void LDC(long iid, Object c) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, c);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IINC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IINC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var the local variable index.
     * @param increment the increment amount.
     */
    @SuppressWarnings("unused")
    public static void IINC(long iid, int var, int increment) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IINC(iid, var, increment);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MULTIANEWARRAY instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MULTIANEWARRAY}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param desc the descriptor of the array
     * @param dims the number of dimensions
     */
    @SuppressWarnings("unused")
    public static void MULTIANEWARRAY(long iid, String desc, int dims) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MULTIANEWARRAY(iid, desc, dims);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LOOKUPSWITCH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LOOKUPSWITCH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param dflt beginning of the default handler block.
     * @param keys the values of the keys.
     * @param labels beginnings of the handler blocks.
     */
    @SuppressWarnings("unused")
    public static void LOOKUPSWITCH(long iid, int dflt, int[] keys, int[] labels) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOOKUPSWITCH(iid, dflt, keys, labels);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the TABLESWITCH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.TABLESWITCH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
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
            long iid, int min, int max, int dflt, int[] labels, int[] values) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.TABLESWITCH(iid, min, max, dflt, labels, values);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFEQ instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFEQ}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFEQ(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFEQ(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFNE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFNE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFNE(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNE(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFLT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFLT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFLT(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLT(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFGE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFGE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFGE(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGE(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFGT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFGT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFGT(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGT(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFLE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFLE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFLE(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLE(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPEQ instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPEQ}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPEQ(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPEQ(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPNE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPNE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPNE(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPNE(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPLT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPLT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPLT(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLT(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPGE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPGE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPGE(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGE(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPGT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPGT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPGT(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGT(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ICMPLE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ICMPLE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ICMPLE(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLE(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ACMPEQ instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ACMPEQ}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ACMPEQ(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPEQ(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IF_ACMPNE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IF_ACMPNE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IF_ACMPNE(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPNE(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GOTO instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GOTO}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void GOTO(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GOTO(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the JSR instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.JSR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void JSR(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.JSR(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFNULL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFNULL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFNULL(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNULL(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IFNONNULL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IFNONNULL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param label the jump destination
     */
    @SuppressWarnings("unused")
    public static void IFNONNULL(long iid, int label) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNONNULL(iid, label);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEVIRTUAL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEVIRTUAL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param invokeId ID that matches an invoke with it's end or exeception
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKEVIRTUAL(
            long iid, long invokeId, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEVIRTUAL(iid, invokeId, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKESPECIAL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKESPECIAL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param invokeId ID that matches an invoke with it's end or exeception
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKESPECIAL(
            long iid, long invokeId, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESPECIAL(iid, invokeId, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEDYNAMIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEDYNAMIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param invokeId ID that matches an invoke with it's end or exeception
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKEDYNAMIC(
            long iid, long invokeId, String owner, String name, String desc, String lambda) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEDYNAMIC(iid, invokeId, owner, name, desc, lambda);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKESTATIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKESTATIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param invokeId ID that matches an invoke with it's end or exeception
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKESTATIC(
            long iid, long invokeId, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESTATIC(iid, invokeId, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEINTERFACE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEINTERFACE}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param invokeId ID that matches an invoke with it's end or exeception
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    @SuppressWarnings("unused")
    public static void INVOKEINTERFACE(
            long iid, long invokeId, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEINTERFACE(iid, invokeId, owner, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETSTATIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETSTATIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param cIdx class index.
     * @param name field name.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void GETSTATIC(long iid, int cIdx, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETSTATIC(iid, cIdx, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the PUTSTATIC instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.PUTSTATIC}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param cIdx class index.
     * @param name field name.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void PUTSTATIC(long iid, int cIdx, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTSTATIC(iid, cIdx, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETFIELD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETFIELD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method}
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param cIdx class index.
     * @param name field name.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void GETFIELD(long iid, int cIdx, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETFIELD(iid, cIdx, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the PUTFIELD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.PUTFIELD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param cIdx class index.
     * @param name field name.
     * @param desc field descriptor.
     */
    @SuppressWarnings("unused")
    public static void PUTFIELD(long iid, int cIdx, String name, String desc) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTFIELD(iid, cIdx, name, desc);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the NEW instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.NEW}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param type the type of the object to be created
     * @param cIdx the class index
     */
    @SuppressWarnings("unused")
    public static void NEW(long iid, String type, int cIdx) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEW(iid, type, cIdx);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CLINIT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CLINIT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param cIdx the class index
     * @param invokeId ID that matches an invoke with it's end or exeception
     */
    @SuppressWarnings("unused")
    public static void CLINIT(long iid, int cIdx, long invokeId) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CLINIT(iid, cIdx, invokeId);
    }

    /**
     * When a method is invoked with reflection, the jvm will compile code that adds all parameters
     * to an array and adds this array to the frame, instead of adding all parameters to the frame.
     *
     * <p>The called method however receives the unpackaged parameters. Since the unpacking code is
     * not instrumented, we add a helper method to do the unpacking for us.
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void UNPACK_INVOKE_PARAMETER(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.UNPACK_INVOKE_PARAMETER(iid);
    }

    @SuppressWarnings("unused")
    public static void SET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, boolean isWideOperand, int modifiers, long iid, long invokeId) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SET_FIELD_REFLECTION(owner, name, desc, reflectFieldName, reflectObjectOwner, isWideOperand, modifiers, iid, invokeId);
    }

    @SuppressWarnings("unused")
    public static void GET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, int modifiers, long iid, long invokeId) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GET_FIELD_REFLECTION(owner, name, desc, reflectFieldName, reflectObjectOwner, modifiers, iid, invokeId);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ANEWARRAY instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ANEWARRAY}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param type type of the array to create.
     */
    @SuppressWarnings("unused")
    public static void ANEWARRAY(long iid, String type) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ANEWARRAY(iid, type);
    }
    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CHECKCAST instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CHECKCAST}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param type the type to check against
     */
    @SuppressWarnings("unused")
    public static void CHECKCAST(long iid, String type) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CHECKCAST(iid, type);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INSTANCEOF instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INSTANCEOF}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param type the type to check against
     */
    @SuppressWarnings("unused")
    public static void INSTANCEOF(long iid, String type) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INSTANCEOF(iid, type);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the BIPUSH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.BIPUSH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param value the value to be pushed.
     */
    @SuppressWarnings("unused")
    public static void BIPUSH(long iid, int value) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BIPUSH(iid, value);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SIPUSH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SIPUSH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param value the value to be pushed.
     */
    @SuppressWarnings("unused")
    public static void SIPUSH(long iid, int value) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SIPUSH(iid, value);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the NEWARRAY instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.NEWARRAY}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param atype the type of the array to be created
     */
    @SuppressWarnings("unused")
    public static void NEWARRAY(long iid, int atype) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEWARRAY(iid, atype);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ILOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ILOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ILOAD(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ILOAD(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LLOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LLOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void LLOAD(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LLOAD(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FLOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FLOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void FLOAD(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FLOAD(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DLOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DLOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void DLOAD(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DLOAD(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ALOAD(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ALOAD(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ISTORE(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISTORE(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void LSTORE(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSTORE(iid, var);
    }


    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FSTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FSTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void FSTORE(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSTORE(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DSTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DSTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void DSTORE(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSTORE(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var local variable index
     */
    @SuppressWarnings("unused")
    public static void ASTORE(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ASTORE(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the RET instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.RET}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param var local variable index to the return address
     */
    @SuppressWarnings("unused")
    public static void RET(long iid, int var) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RET(iid, var);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the NOP instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.NOP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void NOP(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NOP(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ACONST_NULL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ACONST_NULL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ACONST_NULL(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ACONST_NULL(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_M1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_M1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ICONST_M1(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_M1(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     */
    @SuppressWarnings("unused")
    public static void ICONST_0(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_0(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ICONST_1(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_1(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ICONST_2(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_2(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_3 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_3}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ICONST_3(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_3(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_4 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_4}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ICONST_4(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_4(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ICONST_5 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ICONST_5}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ICONST_5(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_5(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LCONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LCONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LCONST_0(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_0(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LCONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LCONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LCONST_1(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_1(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FCONST_0(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_0(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FCONST_1(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_1(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCONST_2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCONST_2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FCONST_2(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_2(iid);
    }


    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCONST_0 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCONST_0}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DCONST_0(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_0(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCONST_1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCONST_1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DCONST_1(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_1(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IALOAD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IALOAD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LALOAD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LALOAD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FALOAD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FALOAD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DALOAD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DALOAD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the AALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.AALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void AALOAD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AALOAD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the BALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.BALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void BALOAD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BALOAD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void CALOAD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CALOAD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SALOAD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SALOAD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void SALOAD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SALOAD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IASTORE(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IASTORE(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LASTORE(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LASTORE(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FASTORE(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FASTORE(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DASTORE(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DASTORE(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the AASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.AASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void AASTORE(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AASTORE(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the BASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.BASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void BASTORE(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BASTORE(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the CASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.CASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void CASTORE(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CASTORE(iid);
    }


    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SASTORE instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SASTORE}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void SASTORE(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SASTORE(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the POP instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.POP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void POP(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the POP2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.POP2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void POP2(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP2(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DUP(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP_X1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP_X1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DUP_X1(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X1(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP_X2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP_X2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method}
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DUP_X2(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X2(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DUP2(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP2_X1 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP2_X1}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DUP2_X1(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X1(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DUP2_X2 instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DUP2_X2}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DUP2_X2(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X2(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SWAP instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SWAP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void SWAP(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SWAP(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IADD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IADD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LADD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LADD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FADD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FADD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DADD instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DADD}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DADD(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DADD(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ISUB(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISUB(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LSUB(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSUB(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FSUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FSUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FSUB(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSUB(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DSUB instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DSUB}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DSUB(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSUB(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IMUL(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IMUL(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LMUL(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LMUL(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FMUL(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FMUL(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DMUL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DMUL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DMUL(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DMUL(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IDIV(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IDIV(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LDIV(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDIV(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FDIV(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FDIV(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DDIV instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DDIV}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DDIV(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DDIV(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IREM(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IREM(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LREM(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LREM(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FREM(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FREM(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DREM instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DREM}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DREM(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DREM(iid);
    }


    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void INEG(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INEG(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LNEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LNEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LNEG(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LNEG(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FNEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FNEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FNEG(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FNEG(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DNEG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DNEG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DNEG(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DNEG(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISHL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISHL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ISHL(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHL(iid);
    }
    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSHL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSHL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LSHL(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHL(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ISHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ISHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ISHR(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHR(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LSHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LSHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LSHR(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHR(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IUSHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IUSHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IUSHR(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IUSHR(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LUSHR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LUSHR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LUSHR(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LUSHR(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IAND instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IAND}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IAND(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IAND(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LAND instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LAND}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LAND(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LAND(iid);
    }


    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IOR instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IOR(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IOR(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LOR instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LOR(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOR(iid);
    }
    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IXOR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IXOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IXOR(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IXOR(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LXOR instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LXOR}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LXOR(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LXOR(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2L instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2L}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void I2L(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2L(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2F instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2F}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void I2F(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2F(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2D instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2D}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void I2D(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2D(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the L2I instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.L2I}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void L2I(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2I(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the L2F instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.L2F}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void L2F(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2F(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the L2D instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.L2D}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void L2D(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2D(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the F2I instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.F2I}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void F2I(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2I(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the F2L instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.F2L}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void F2L(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2L(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the F2D instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.F2D}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void F2D(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2D(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the D2I instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.D2I}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void D2I(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2I(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the D2L instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.D2L}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void D2L(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2L(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the D2F instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.D2F}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void D2F(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2F(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2B instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2B}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void I2B(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2B(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2C instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2C}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void I2C(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2C(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the I2S instruction is executed in an instrumented
     * region, this method is called. Parameters provide additional extracted information necessary
     * for handling this instruction symbolically. For more information regarding the handling of
     * this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.I2S}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void I2S(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2S(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LCMP instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LCMP}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LCMP(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCMP(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCMPL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCMPL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FCMPL(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPL(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FCMPG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FCMPG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FCMPG(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPG(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCMPL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCMPL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DCMPL(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPL(iid);
    }


    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DCMPG instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DCMPG}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DCMPG(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPG(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the IRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.IRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void IRETURN(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IRETURN(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the LRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.LRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void LRETURN(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LRETURN(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the FRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.FRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void FRETURN(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FRETURN(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the DRETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.DRETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void DRETURN(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DRETURN(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ARETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ARETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ARETURN(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARETURN(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the RETURN instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.RETURN}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void RETURN(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RETURN(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ARRAYLENGTH instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ARRAYLENGTH}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ARRAYLENGTH(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARRAYLENGTH(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the ATHROW instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.ATHROW}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void ATHROW(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ATHROW(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MONITORENTER instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MONITORENTER}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void MONITORENTER(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITORENTER(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the MONITOREXIT instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.MONITOREXIT}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void MONITOREXIT(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITOREXIT(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_double instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_double}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_double(double v, long iid, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_double(iid, v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_long instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_long}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_long(long v, long iid, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_long(iid, v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_Object instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_Object}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_Object(Object v, long iid,  int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_Object(iid, v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_boolean instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_boolean}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_boolean(boolean v, long iid, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_boolean(iid, v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_byte instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_byte}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_byte(byte v, long iid, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_byte(iid, v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_char instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_char}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_char(char v, long iid, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_char(iid, v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_float instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_float}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_float(float v, long iid, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_float(iid, v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_int instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_int}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_int(int v, long iid, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_int(iid, v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_short instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_short}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_short(short v, long iid, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_short(iid, v, i);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the GETVALUE_void instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.GETVALUE_void}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void GETVALUE_void(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_void(iid);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEMETHOD_EXCEPTION instruction is executed
     * in an instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEMETHOD_EXCEPTION}. The instruction is handled in
     * the {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param invokeId ID that matches an invoke with it's end or exeception
     */
    @SuppressWarnings("unused")
    public static void INVOKEMETHOD_EXCEPTION(long iid, long invokeId) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEMETHOD_EXCEPTION(iid, invokeId);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKEMETHOD_END instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKEMETHOD_END}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param invokeId ID that matches an invoke with it's end or exeception
     */
    @SuppressWarnings("unused")
    public static void INVOKEMETHOD_END(long iid, long invokeId) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEMETHOD_END(iid, invokeId);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the INVOKECLINIT_END instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.INVOKECLINIT_END}. The instruction is handled in the
     * {@link de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor
     * SymbolicInstructionProcessor} that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param invokeId ID that matches an invoke with it's end or exeception
     */
    @SuppressWarnings("unused")
    public static void INVOKECLINIT_END(long iid, long invokeId) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKECLINIT_END(iid, invokeId);
    }

    /**
     * Method used to interact between the concrete and symbolic execution. The call to this method
     * is added during instrumentation. Each time the SPECIAL instruction is executed in an
     * instrumented region, this method is called. Parameters provide additional extracted
     * information necessary for handling this instruction symbolically. For more information
     * regarding the handling of this instruction look at the Instruction itself: {@link
     * de.uzl.its.swat.symbolic.instruction.SPECIAL}. The instruction is handled in the {@link
     * de.uzl.its.swat.symbolic.processor.SymbolicInstructionProcessor SymbolicInstructionProcessor}
     * that invokes the corresponding visitor method
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param i marker used in symbolic interpreter for example for branch detection.
     */
    @SuppressWarnings("unused")
    public static void SPECIAL(long iid, int i) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SPECIAL(iid, i);
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
     *
     * @param iid The unique instruction ID set during instrumentation.
     */
    @SuppressWarnings("unused")
    public static void MAKE_SYMBOLIC(long iid) {
        InstructionProcessor instructionProcessor =
                ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MAKE_SYMBOLIC(iid);
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
    public static void LOOP_BEGIN(long iid) {
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
    public static void LOOP_END(long iid) {
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
