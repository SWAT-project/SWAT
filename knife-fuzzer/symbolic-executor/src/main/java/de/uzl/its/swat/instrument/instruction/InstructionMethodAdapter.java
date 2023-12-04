package de.uzl.its.swat.instrument.instruction;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import de.uzl.its.swat.instrument.TryCatchBlock;
import de.uzl.its.swat.instrument.Utils;
import de.uzl.its.swat.logger.ClassNames;
import de.uzl.its.swat.logger.ObjectInfo;
import java.util.Arrays;
import java.util.LinkedList;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** A visitor to visit a Java method */
public class InstructionMethodAdapter extends MethodVisitor implements Opcodes {
    boolean isInit;
    boolean isSuperInitCalled;
    LinkedList<TryCatchBlock> tryCatchBlocks;
    boolean calledNew = false;

    private final GlobalStateForInstrumentation instrumentationState;
    private final ClassNames classNames;
    private final String methodName;
    private final Config config = Config.instance();

    /**
     * Constructor that calls the super from the default MethodVisitor and sets some locals
     *
     * @param mv The MethodVisitor that should be used
     * @param isInit Determines if the method is an initializer method
     * @param instrumentationState The current state of instrumentation
     * @param classNames The class names
     * @param methodName The Method name
     */
    public InstructionMethodAdapter(
            MethodVisitor mv,
            boolean isInit,
            GlobalStateForInstrumentation instrumentationState,
            ClassNames classNames,
            String methodName) {
        super(ASM9, mv);
        this.isInit = isInit;
        this.isSuperInitCalled = false;
        tryCatchBlocks = new LinkedList<TryCatchBlock>();

        this.instrumentationState = instrumentationState;
        this.classNames = classNames;
        this.methodName = methodName;
    }

    /** Starts the visit of the method's code, if any (i.e. non abstract method). */
    @Override
    public void visitCode() {
        if (methodName.equals("<clinit>")) {
            addSpecialInsn(mv, 2);
        }
        instrumentationState.incMid();
        mv.visitCode();
    }

    /** Push a value onto the stack. */
    private static void addBipushInsn(MethodVisitor mv, int val) {
        Utils.addBipushInsn(mv, val);
    }

    /** Add a GETVALUE call to synchronize the top stack with that of the symbolic stack. */
    private void addValueReadInsn(MethodVisitor mv, String desc, String methodNamePrefix) {
        Utils.addValueReadInsn(mv, desc, methodNamePrefix);
    }

    /** Add a special probe instruction. */
    private void addSpecialInsn(MethodVisitor mv, int val) {
        Utils.addSpecialInsn(mv, val);
    }

    private void addInsn(MethodVisitor mv, String insn, boolean exception, int opcode) {
        String desc = "()V";
        if (config.isInstructionIds()) {
            desc = "(II)V";
            addBipushInsn(mv, instrumentationState.incAndGetId());
            addBipushInsn(mv, instrumentationState.getMid());
        } else if (exception) {
            desc = "(I)V";
            addBipushInsn(mv, instrumentationState.incAndGetId());
        }
        mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), insn, desc, false);
        mv.visitInsn(opcode);
    }

    /** Add var insn and its instrumentation code. */
    private void addVarInsn(MethodVisitor mv, int var, String insn, int opcode) {
        String desc = "(I)V";
        if (config.isInstructionIds()) {
            desc = "(III)V";
            addBipushInsn(mv, instrumentationState.incAndGetId());
            addBipushInsn(mv, instrumentationState.getMid());
        }
        addBipushInsn(mv, var);
        mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), insn, desc, false);

        mv.visitVarInsn(opcode, var);
    }

    private void addTypeInsn(MethodVisitor mv, String type, int opcode, String name) {
        String desc = "(ILjava/lang/String;)V";
        addBipushInsn(mv, instrumentationState.incAndGetId());
        if (config.isInstructionIds()) {
            desc = "(IILjava/lang/String;)V";
            addBipushInsn(mv, instrumentationState.getMid());
        }
        mv.visitLdcInsn(type);
        mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), name, desc, false);
        mv.visitTypeInsn(opcode, type);
    }

    /**
     * Visits a line number declaration.
     *
     * @param lineNumber- a line number. This number refers to the source file from which the class
     *     was compiled.
     * @param label- the first instruction corresponding to this line number.
     */
    @Override
    public void visitLineNumber(int lineNumber, Label label) {
        mv.visitLineNumber(lineNumber, label);
    }

    /**
     * Visits a zero operand instruction.
     *
     * @param opcode - the opcode of the instruction to be visited. This opcode is either NOP,
     *     ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
     *     LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, IALOAD, LALOAD,
     *     FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE, DASTORE,
     *     AASTORE, BASTORE, CASTORE, SASTORE, POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1,
     *     DUP2_X2, SWAP, IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL,
     *     IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL, LSHL, ISHR,
     *     LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR, I2L, I2F, I2D, L2I, L2F, L2D, F2I,
     *     F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN,
     *     LRETURN, FRETURN, DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW, MONITORENTER, or
     *     MONITOREXIT.
     */
    @Override
    public void visitInsn(int opcode) {
        boolean ex;
        switch (opcode) {
            case NOP:
                ex = false;
                addInsn(mv, "NOP", ex, opcode);
                break;
            case ACONST_NULL:
                ex = false;
                addInsn(mv, "ACONST_NULL", ex, opcode);
                break;
            case ICONST_M1:
                ex = false;
                addInsn(mv, "ICONST_M1", ex, opcode);
                break;
            case ICONST_0:
                ex = false;
                addInsn(mv, "ICONST_0", ex, opcode);
                break;
            case ICONST_1:
                ex = false;
                addInsn(mv, "ICONST_1", ex, opcode);
                break;
            case ICONST_2:
                ex = false;
                addInsn(mv, "ICONST_2", ex, opcode);
                break;
            case ICONST_3:
                ex = false;
                addInsn(mv, "ICONST_3", ex, opcode);
                break;
            case ICONST_4:
                ex = false;
                addInsn(mv, "ICONST_4", ex, opcode);
                break;
            case ICONST_5:
                ex = false;
                addInsn(mv, "ICONST_5", ex, opcode);
                break;
            case LCONST_0:
                ex = false;
                addInsn(mv, "LCONST_0", ex, opcode);
                break;
            case LCONST_1:
                ex = false;
                addInsn(mv, "LCONST_1", ex, opcode);
                break;
            case FCONST_0:
                ex = false;
                addInsn(mv, "FCONST_0", ex, opcode);
                break;
            case FCONST_1:
                ex = false;
                addInsn(mv, "FCONST_1", ex, opcode);
                break;
            case FCONST_2:
                ex = false;
                addInsn(mv, "FCONST_2", ex, opcode);
                break;
            case DCONST_0:
                ex = false;
                addInsn(mv, "DCONST_0", ex, opcode);
                break;
            case DCONST_1:
                ex = false;
                addInsn(mv, "DCONST_1", ex, opcode);
                break;
            case IALOAD:
                ex = true;
                addInsn(mv, "IALOAD", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "I", "GETVALUE_");
                break;
            case LALOAD:
                ex = true;
                addInsn(mv, "LALOAD", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "J", "GETVALUE_");
                break;
            case FALOAD:
                ex = true;
                addInsn(mv, "FALOAD", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "F", "GETVALUE_");
                break;
            case DALOAD:
                ex = true;
                addInsn(mv, "DALOAD", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "D", "GETVALUE_");
                break;
            case AALOAD:
                ex = true;
                addInsn(mv, "AALOAD", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "Ljava/lang/Object;", "GETVALUE_");
                break;
            case BALOAD:
                ex = true;
                addInsn(mv, "BALOAD", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "B", "GETVALUE_");
                break;
            case CALOAD:
                ex = true;
                addInsn(mv, "CALOAD", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "C", "GETVALUE_");
                break;
            case SALOAD:
                ex = true;
                addInsn(mv, "SALOAD", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "S", "GETVALUE_");
                break;
            case IASTORE:
                ex = true;
                addInsn(mv, "IASTORE", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case LASTORE:
                ex = true;
                addInsn(mv, "LASTORE", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case FASTORE:
                ex = true;
                addInsn(mv, "FASTORE", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case DASTORE:
                ex = true;
                addInsn(mv, "DASTORE", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case AASTORE:
                ex = true;
                addInsn(mv, "AASTORE", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case BASTORE:
                ex = true;
                addInsn(mv, "BASTORE", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case CASTORE:
                ex = true;
                addInsn(mv, "CASTORE", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case SASTORE:
                ex = true;
                addInsn(mv, "SASTORE", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case POP:
                ex = false;
                addInsn(mv, "POP", ex, opcode);
                break;
            case POP2:
                ex = false;
                addInsn(mv, "POP2", ex, opcode);
                break;
            case DUP:
                ex = false;
                addInsn(mv, "DUP", ex, opcode);
                break;
            case DUP_X1:
                ex = false;
                addInsn(mv, "DUP_X1", ex, opcode);
                break;
            case DUP_X2:
                ex = false;
                addInsn(mv, "DUP_X2", ex, opcode);
                break;
            case DUP2:
                ex = false;
                addInsn(mv, "DUP2", ex, opcode);
                break;
            case DUP2_X1:
                ex = false;
                addInsn(mv, "DUP2_X1", ex, opcode);
                break;
            case DUP2_X2:
                ex = false;
                addInsn(mv, "DUP2_X2", ex, opcode);
                break;
            case SWAP:
                ex = false;
                addInsn(mv, "SWAP", ex, opcode);
                break;
            case IADD:
                ex = false;
                addInsn(mv, "IADD", ex, opcode);
                break;
            case LADD:
                ex = false;
                addInsn(mv, "LADD", ex, opcode);
                break;
            case FADD:
                ex = false;
                addInsn(mv, "FADD", ex, opcode);
                break;
            case DADD:
                ex = false;
                addInsn(mv, "DADD", ex, opcode);
                break;
            case ISUB:
                ex = false;
                addInsn(mv, "ISUB", ex, opcode);
                break;
            case LSUB:
                ex = false;
                addInsn(mv, "LSUB", ex, opcode);
                break;
            case FSUB:
                ex = false;
                addInsn(mv, "FSUB", ex, opcode);
                break;
            case DSUB:
                ex = false;
                addInsn(mv, "DSUB", ex, opcode);
                break;
            case IMUL:
                ex = false;
                addInsn(mv, "IMUL", ex, opcode);
                break;
            case LMUL:
                ex = false;
                addInsn(mv, "LMUL", ex, opcode);
                break;
            case FMUL:
                ex = false;
                addInsn(mv, "FMUL", ex, opcode);
                break;
            case DMUL:
                ex = false;
                addInsn(mv, "DMUL", ex, opcode);
                break;
            case IDIV:
                ex = true;
                addInsn(mv, "IDIV", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case LDIV:
                ex = true;
                addInsn(mv, "LDIV", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case FDIV:
                ex = false;
                addInsn(mv, "FDIV", ex, opcode);
                break;
            case DDIV:
                ex = false;
                addInsn(mv, "DDIV", ex, opcode);
                break;
            case IREM:
                ex = true;
                addInsn(mv, "IREM", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case LREM:
                ex = true;
                addInsn(mv, "LREM", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case FREM:
                ex = false;
                addInsn(mv, "FREM", ex, opcode);
                break;
            case DREM:
                ex = false;
                addInsn(mv, "DREM", ex, opcode);
                break;
            case INEG:
                ex = false;
                addInsn(mv, "INEG", ex, opcode);
                break;
            case LNEG:
                ex = false;
                addInsn(mv, "LNEG", ex, opcode);
                break;
            case FNEG:
                ex = false;
                addInsn(mv, "FNEG", ex, opcode);
                break;
            case DNEG:
                ex = false;
                addInsn(mv, "DNEG", ex, opcode);
                break;
            case ISHL:
                ex = false;
                addInsn(mv, "ISHL", ex, opcode);
                break;
            case LSHL:
                ex = false;
                addInsn(mv, "LSHL", ex, opcode);
                break;
            case ISHR:
                ex = false;
                addInsn(mv, "ISHR", ex, opcode);
                break;
            case LSHR:
                ex = false;
                addInsn(mv, "LSHR", ex, opcode);
                break;
            case IUSHR:
                ex = false;
                addInsn(mv, "IUSHR", ex, opcode);
                break;
            case LUSHR:
                ex = false;
                addInsn(mv, "LUSHR", ex, opcode);
                break;
            case IAND:
                ex = false;
                addInsn(mv, "IAND", ex, opcode);
                break;
            case LAND:
                ex = false;
                addInsn(mv, "LAND", ex, opcode);
                break;
            case IOR:
                ex = false;
                addInsn(mv, "IOR", ex, opcode);
                break;
            case LOR:
                ex = false;
                addInsn(mv, "LOR", ex, opcode);
                break;
            case IXOR:
                ex = false;
                addInsn(mv, "IXOR", ex, opcode);
                break;
            case LXOR:
                ex = false;
                addInsn(mv, "LXOR", ex, opcode);
                break;
            case I2L:
                ex = false;
                addInsn(mv, "I2L", ex, opcode);
                break;
            case I2F:
                ex = false;
                addInsn(mv, "I2F", ex, opcode);
                break;
            case I2D:
                ex = false;
                addInsn(mv, "I2D", ex, opcode);
                break;
            case L2I:
                ex = false;
                addInsn(mv, "L2I", ex, opcode);
                break;
            case L2F:
                ex = false;
                addInsn(mv, "L2F", ex, opcode);
                break;
            case L2D:
                ex = false;
                addInsn(mv, "L2D", ex, opcode);
                break;
            case F2I:
                ex = false;
                addInsn(mv, "F2I", ex, opcode);
                break;
            case F2L:
                ex = false;
                addInsn(mv, "F2L", ex, opcode);
                break;
            case F2D:
                ex = false;
                addInsn(mv, "F2D", ex, opcode);
                break;
            case D2I:
                ex = false;
                addInsn(mv, "D2I", ex, opcode);
                break;
            case D2L:
                ex = false;
                addInsn(mv, "D2L", ex, opcode);
                break;
            case D2F:
                ex = false;
                addInsn(mv, "D2F", ex, opcode);
                break;
            case I2B:
                ex = false;
                addInsn(mv, "I2B", ex, opcode);
                break;
            case I2C:
                ex = false;
                addInsn(mv, "I2C", ex, opcode);
                break;
            case I2S:
                ex = false;
                addInsn(mv, "I2S", ex, opcode);
                break;
            case LCMP:
                ex = false;
                addInsn(mv, "LCMP", ex, opcode);
                break;
            case FCMPL:
                ex = false;
                addInsn(mv, "FCMPL", ex, opcode);
                break;
            case FCMPG:
                ex = false;
                addInsn(mv, "FCMPG", ex, opcode);
                break;
            case DCMPL:
                ex = false;
                addInsn(mv, "DCMPL", ex, opcode);
                break;
            case DCMPG:
                ex = false;
                addInsn(mv, "DCMPG", ex, opcode);
                break;
            case IRETURN:
                ex = true;
                addInsn(mv, "IRETURN", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case LRETURN:
                ex = true;
                addInsn(mv, "LRETURN", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case FRETURN:
                ex = true;
                addInsn(mv, "FRETURN", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case DRETURN:
                ex = true;
                addInsn(mv, "DRETURN", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case ARETURN:
                ex = true;
                addInsn(mv, "ARETURN", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case RETURN:
                ex = true;
                addInsn(mv, "RETURN", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case ARRAYLENGTH:
                ex = true;
                addInsn(mv, "ARRAYLENGTH", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "I", "GETVALUE_");
                break;
            case ATHROW:
                ex = true;
                addInsn(mv, "ATHROW", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case MONITORENTER:
                ex = true;
                addInsn(mv, "MONITORENTER", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case MONITOREXIT:
                ex = true;
                addInsn(mv, "MONITOREXIT", ex, opcode);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            default:
                throw new RuntimeException("Unknown instruction opcode " + opcode);
        }
    }

    /**
     * Visits a local variable instruction. A local variable instruction is an instruction that
     * loads or stores the value of a local variable.
     *
     * @param opcode the opcode of the local variable instruction to be visited. This opcode is
     *     either ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE or RET.
     * @param var the operand of the instruction to be visited. This operand is the index of a local
     *     variable.
     */
    @Override
    public void visitVarInsn(int opcode, int var) {
        switch (opcode) {
            case ILOAD:
                addVarInsn(mv, var, "ILOAD", opcode);
                addValueReadInsn(mv, "I", "GETVALUE_");
                break;
            case LLOAD:
                addVarInsn(mv, var, "LLOAD", opcode);
                addValueReadInsn(mv, "J", "GETVALUE_");
                break;
            case FLOAD:
                addVarInsn(mv, var, "FLOAD", opcode);
                addValueReadInsn(mv, "F", "GETVALUE_");
                break;
            case DLOAD:
                addVarInsn(mv, var, "DLOAD", opcode);
                addValueReadInsn(mv, "D", "GETVALUE_");
                break;
            case ALOAD:
                addVarInsn(mv, var, "ALOAD", opcode);
                if (!(var == 0 && isInit && !isSuperInitCalled)) {
                    addValueReadInsn(mv, "Ljava/lang/Object;", "GETVALUE_");
                }
                break;
            case ISTORE:
                addVarInsn(mv, var, "ISTORE", opcode);
                break;
            case LSTORE:
                addVarInsn(mv, var, "LSTORE", opcode);
                break;
            case FSTORE:
                addVarInsn(mv, var, "FSTORE", opcode);
                break;
            case DSTORE:
                addVarInsn(mv, var, "DSTORE", opcode);
                break;
            case ASTORE:
                addVarInsn(mv, var, "ASTORE", opcode);
                break;
            case RET:
                addVarInsn(mv, var, "RET", opcode);
                break;
            default:
                throw new RuntimeException("Unknown var insn");
        }
    }

    /**
     * Visits an instruction with a single int operand.
     *
     * @param opcode the opcode of the instruction to be visited. This opcode is either BIPUSH,
     *     SIPUSH or NEWARRAY.
     * @param operand the operand of the instruction to be visited. When opcode is BIPUSH, operand
     *     value should be between Byte.MIN_VALUE and Byte.MAX_VALUE. When opcode is SIPUSH, operand
     *     value should be between Short.MIN_VALUE and Short.MAX_VALUE. When opcode is NEWARRAY,
     *     operand value should be one of Opcodes.T_BOOLEAN, Opcodes.T_CHAR, Opcodes.T_FLOAT,
     *     Opcodes.T_DOUBLE, Opcodes.T_BYTE, Opcodes.T_SHORT, Opcodes.T_INT or Opcodes.T_LONG.
     */
    @Override
    public void visitIntInsn(int opcode, int operand) {

        String desc = "(I)V";
        if (config.isInstructionIds()) {
            desc = "(III)V";
            addBipushInsn(mv, instrumentationState.incAndGetId());
            addBipushInsn(mv, instrumentationState.getMid());
        } else if (opcode == NEWARRAY) {
            desc = "(II)V";
            addBipushInsn(mv, instrumentationState.incAndGetId());
        }
        switch (opcode) {
            case BIPUSH:
                addBipushInsn(mv, operand);
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "BIPUSH", desc, false);
                break;
            case SIPUSH:
                addBipushInsn(mv, operand);
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "SIPUSH", desc, false);
                break;
            case NEWARRAY:
                addBipushInsn(mv, operand);
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "NEWARRAY", desc, false);
                mv.visitIntInsn(opcode, operand);
                addSpecialInsn(mv, 0); // for non-exceptional path
                return;
            default:
                throw new RuntimeException("Unknown int instruction opcode " + opcode);
        }
        mv.visitIntInsn(opcode, operand);
    }

    /**
     * Visits a type instruction. A type instruction is an instruction that takes the internal name
     * of a class as parameter.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode is either NEW,
     *     ANEWARRAY, CHECKCAST or INSTANCEOF.
     * @param type the operand of the instruction to be visited. This operand must be the internal
     *     name of an object or array class (see Type.getInternalName()).
     */
    @Override
    public void visitTypeInsn(int opcode, String type) {
        switch (opcode) {
            case NEW:
                String desc = "(ILjava/lang/String;I)V";
                addBipushInsn(mv, instrumentationState.incAndGetId());
                if (config.isInstructionIds()) {
                    desc = "(IILjava/lang/String;I)V";
                    addBipushInsn(mv, instrumentationState.getMid());
                }
                mv.visitLdcInsn(type);
                int cIdx = classNames.get(type);
                addBipushInsn(mv, cIdx);
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "NEW", desc, false);
                mv.visitTypeInsn(opcode, type);
                addSpecialInsn(mv, 0); // for non-exceptional path

                calledNew = true;
                break;
            case ANEWARRAY:
                addTypeInsn(mv, type, opcode, "ANEWARRAY");
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case CHECKCAST:
                addTypeInsn(mv, type, opcode, "CHECKCAST");
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case INSTANCEOF:
                addTypeInsn(mv, type, opcode, "INSTANCEOF");
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, "I", "GETVALUE_");
                break;
            default:
                throw new RuntimeException("Unknown type instruction opcode " + opcode);
        }
    }

    /**
     * Visits a field instruction. A field instruction is an instruction that loads or stores the
     * value of a field of an object.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode is either
     *     GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
     * @param owner the internal name of the field's owner class (see Type.getInternalName()).
     * @param name the field's name.
     * @param desc the field's descriptor (see Type).
     */
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        String d = "(IIILjava/lang/String;)V";
        addBipushInsn(mv, instrumentationState.incAndGetId());
        if (config.isInstructionIds()) {
            d = "(IIIILjava/lang/String;)V";
            addBipushInsn(mv, instrumentationState.getMid());
        }
        int cIdx = classNames.get(owner);
        addBipushInsn(mv, cIdx);
        ObjectInfo tmp = classNames.get(cIdx);
        switch (opcode) {
            case GETSTATIC:
                int fIdx = tmp.getIdx(name, true);
                addBipushInsn(mv, fIdx);
                mv.visitLdcInsn(desc);

                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "GETSTATIC", d, false);

                mv.visitFieldInsn(opcode, owner, name, desc);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, owner + ":" + name + ":" + desc, "GETVALUE_");
                break;
            case PUTSTATIC:
                fIdx = tmp.getIdx(name, true);
                addBipushInsn(mv, fIdx);
                mv.visitLdcInsn(desc);

                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "PUTSTATIC", d, false);
                mv.visitFieldInsn(opcode, owner, name, desc);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            case GETFIELD:
                fIdx = tmp.getIdx(name, false);
                addBipushInsn(mv, fIdx);
                mv.visitLdcInsn(desc);

                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "GETFIELD", d, false);
                mv.visitFieldInsn(opcode, owner, name, desc);
                addSpecialInsn(mv, 0); // for non-exceptional path
                addValueReadInsn(mv, owner + ":" + name + ":" + desc, "GETVALUE_");
                break;
            case PUTFIELD:
                fIdx = tmp.getIdx(name, false);
                addBipushInsn(mv, fIdx);
                mv.visitLdcInsn(desc);

                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "PUTFIELD", d, false);
                mv.visitFieldInsn(opcode, owner, name, desc);
                addSpecialInsn(mv, 0); // for non-exceptional path
                break;
            default:
                throw new RuntimeException("Unknown field access opcode " + opcode);
        }
    }

    private String getMethodNameByOpcode(int opcode) {
        switch (opcode) {
            case INVOKESPECIAL:
                return "INVOKESPECIAL";
            case INVOKESTATIC:
                return "INVOKESTATIC";
            case INVOKEINTERFACE:
                return "INVOKEINTERFACE";
            case INVOKEVIRTUAL:
                return "INVOKEVIRTUAL";
            case INVOKEDYNAMIC:
                return "INVOKEDYNAMIC";
            default:
                throw new RuntimeException("Unknown opcode for method");
        }
    }

    private void addMethodWithTryCatch(
            int opcode, String owner, String name, String desc, boolean itf) {
        String d = "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
        addBipushInsn(mv, instrumentationState.incAndGetId());
        if (config.isInstructionIds()) {
            d = "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
            addBipushInsn(mv, instrumentationState.getMid());
        }
        mv.visitLdcInsn(owner);
        mv.visitLdcInsn(name);
        mv.visitLdcInsn(desc);
        mv.visitMethodInsn(
                INVOKESTATIC, config.getAnalysisClass(), getMethodNameByOpcode(opcode), d, false);
        // Wrap the method call in a try-catch block
        Label begin = new Label();
        Label handler = new Label();
        Label end = new Label();

        tryCatchBlocks.addFirst(new TryCatchBlock(begin, handler, handler, null));

        mv.visitLabel(begin);
        mv.visitMethodInsn(opcode, owner, name, desc, itf);
        mv.visitJumpInsn(GOTO, end);

        mv.visitLabel(handler);
        mv.visitMethodInsn(
                INVOKESTATIC, config.getAnalysisClass(), "INVOKEMETHOD_EXCEPTION", "()V", false);
        mv.visitInsn(ATHROW);

        mv.visitLabel(end);
        mv.visitMethodInsn(
                INVOKESTATIC, config.getAnalysisClass(), "INVOKEMETHOD_END", "()V", false);

        addValueReadInsn(mv, owner + ":" + name + ":" + desc, "GETVALUE_");
    }

    /**
     * Visits an invokedynamic instruction.
     *
     * @param name the method's name.
     * @param desc the method's descriptor (see Type).
     * @param bsm the bootstrap method.
     * @param bsmArgs the bootstrap method constant arguments. Each argument must be an Integer,
     *     Float, Long, Double, String, Type, Handle or ConstantDynamic value. This method is
     *     allowed to modify the content of the array so a caller should expect that this array may
     *     change.
     */
    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        String d = "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
        // increment and pushes the instruction id to the stack using the mv
        addBipushInsn(mv, instrumentationState.incAndGetId());
        if (config.isInstructionIds()) {
            d = "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
            // push the current method id to the stack using mv
            addBipushInsn(mv, instrumentationState.getMid());
        }
        // load the owner to the stack (param 1)
        mv.visitLdcInsn(bsm.getOwner());
        // load the name to the stack (param 2)
        mv.visitLdcInsn(name);
        // load the description to the stack (param 3)
        mv.visitLdcInsn(desc);
        // load the lambda method description (param 4.1)
        mv.visitLdcInsn(Arrays.toString(bsmArgs));
        // call de.uzl.its.swat.logger.DJVM (default)
        mv.visitMethodInsn(
                INVOKESTATIC, // opcode
                config.getAnalysisClass(), // owner
                getMethodNameByOpcode(INVOKEDYNAMIC), // name
                d, // desc
                false); // is interface?

        // Wrap the method call in a try-catch block
        Label begin = new Label();
        Label handler = new Label();
        Label end = new Label();

        tryCatchBlocks.addFirst(new TryCatchBlock(begin, handler, handler, null));

        mv.visitLabel(begin); // for adding try catch?
        mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs); // calling the actual method
        mv.visitJumpInsn(GOTO, end); // still try catch

        mv.visitLabel(handler);
        mv.visitMethodInsn(
                INVOKESTATIC, config.getAnalysisClass(), "INVOKEMETHOD_EXCEPTION", "()V", false);
        mv.visitInsn(ATHROW);

        mv.visitLabel(end);
        mv.visitMethodInsn(
                INVOKESTATIC, config.getAnalysisClass(), "INVOKEMETHOD_END", "()V", false);

        addValueReadInsn(mv, bsm.getOwner() + ":" + name + ":" + desc, "GETVALUE_");
    }

    /**
     * Visits a method instruction. A method instruction is an instruction that invokes a method.
     *
     * @param opcode the opcode of the type instruction to be visited. This opcode is either
     *     INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
     * @param owner the internal name of the method's owner class (see Type.getInternalName()).
     * @param name the method's name.
     * @param desc the method's descriptor (see Type).
     * @param itf if the method's owner class is an interface.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (opcode == INVOKESPECIAL && name.equals("<init>")) {
            if (isInit) {
                // This code is already inside an init method.
                //
                // Constructor calls to <init> method of the super class. If this is the
                // case, there is no need to wrap the method call in try catch block as
                // it uses uninitialized this object.
                isSuperInitCalled = true;

                // if (!desc.equals("()V") || true) {
                addSpecialInsn(mv, 420);

                String d = "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
                // increment and pushes the instruction id to the stack using the mv
                addBipushInsn(mv, instrumentationState.incAndGetId());
                if (config.isInstructionIds()) {
                    d = "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
                    // push the current method id to the stack using mv
                    addBipushInsn(mv, instrumentationState.getMid());
                }

                mv.visitLdcInsn(owner);
                mv.visitLdcInsn(name);
                mv.visitLdcInsn(desc);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        config.getAnalysisClass(),
                        getMethodNameByOpcode(opcode),
                        d,
                        false);

                mv.visitMethodInsn(opcode, owner, name, desc, itf);

                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "INVOKEMETHOD_END", "()V", false);
                addValueReadInsn(mv, owner + ":" + name + ":" + desc, "GETVALUE_");

                addSpecialInsn(mv, 421);
                // } else {
                //  mv.visitMethodInsn(opcode, owner, name, desc, itf);
                // }

                //
                calledNew = false;

            } else {
                addMethodWithTryCatch(opcode, owner, name, desc, itf);
                if (calledNew) {
                    calledNew = false;
                    addValueReadInsn(mv, "Ljava/lang/Object;", "GETVALUE_");
                }
                // throw new RuntimeException("I will never be called, because name==<init> and thus
                // isInit is true " + name + ", " + isInit);
            }
        } else {
            addMethodWithTryCatch(opcode, owner, name, desc, itf);
        }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        int iid3;
        String desc = "(II)V";
        addBipushInsn(mv, iid3 = instrumentationState.incAndGetId());
        if (config.isInstructionIds()) {
            desc = "(III)V";
            addBipushInsn(mv, instrumentationState.getMid());
        }
        addBipushInsn(mv, System.identityHashCode(label)); // label.getOffset()
        switch (opcode) {
            case IFEQ:
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "IFEQ", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IFNE:
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "IFNE", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IFLT:
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "IFLT", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IFGE:
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "IFGE", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IFGT:
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "IFGT", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IFLE:
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "IFLE", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IF_ICMPEQ:
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "IF_ICMPEQ", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IF_ICMPNE:
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "IF_ICMPNE", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IF_ICMPLT:
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "IF_ICMPLT", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IF_ICMPGE:
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "IF_ICMPGE", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IF_ICMPGT:
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "IF_ICMPGT", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IF_ICMPLE:
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "IF_ICMPLE", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IF_ACMPEQ:
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "IF_ACMPEQ", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IF_ACMPNE:
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "IF_ACMPNE", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case GOTO:
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "GOTO", desc, false);
                mv.visitJumpInsn(opcode, label);
                break;
            case JSR:
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "JSR", desc, false);
                mv.visitJumpInsn(opcode, label);
                break;
            case IFNULL:
                mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "IFNULL", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            case IFNONNULL:
                mv.visitMethodInsn(
                        INVOKESTATIC, config.getAnalysisClass(), "IFNONNULL", desc, false);
                mv.visitJumpInsn(opcode, label);
                addSpecialInsn(mv, 1); // for true path
                break;
            default:
                throw new RuntimeException("Unknown jump opcode " + opcode);
        }
    }

    @Override
    public void visitLdcInsn(Object cst) {

        String desc = "(I";
        addBipushInsn(mv, instrumentationState.incAndGetId());
        if (config.isInstructionIds()) {
            desc = "(II";
            addBipushInsn(mv, instrumentationState.getMid());
        }
        mv.visitLdcInsn(cst);
        if (cst instanceof Integer) {
            mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "LDC", desc + "I)V", false);
        } else if (cst instanceof Long) {
            mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "LDC", desc + "J)V", false);
        } else if (cst instanceof Float) {
            mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "LDC", desc + "F)V", false);
        } else if (cst instanceof Double) {
            mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "LDC", desc + "D)V", false);
        } else if (cst instanceof String) {
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    config.getAnalysisClass(),
                    "LDC",
                    desc + "Ljava/lang/String;)V",
                    false);
        } else {
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    config.getAnalysisClass(),
                    "LDC",
                    desc + "Ljava/lang/Object;)V",
                    false);
        }
        mv.visitLdcInsn(cst);
        addSpecialInsn(mv, 0); // for non-exceptional path
    }

    @Override
    public void visitIincInsn(int var, int increment) {

        String desc = "(II)V";
        if (config.isInstructionIds()) {
            desc = "(IIII)V";
            addBipushInsn(mv, instrumentationState.incAndGetId());
            addBipushInsn(mv, instrumentationState.getMid());
        }
        addBipushInsn(mv, var);
        addBipushInsn(mv, increment);
        mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "IINC", desc, false);
        mv.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        int iid3;

        String desc = "(IIII[I[I)V"; // Add an extra array for case values
        addBipushInsn(mv, iid3 = instrumentationState.incAndGetId());
        if (config.isInstructionIds()) {
            desc = "(IIIII[I[I)V"; // Adjust descriptor for the added array
            addBipushInsn(mv, instrumentationState.getMid());
        }
        addBipushInsn(mv, min);
        addBipushInsn(mv, max);
        addBipushInsn(mv, System.identityHashCode(dflt)); // label.getOffset()

        addBipushInsn(mv, labels.length);
        mv.visitIntInsn(NEWARRAY, T_INT); // Creates an array with `labels.length` size

        for (int i = 0; i < labels.length; i++) {
            mv.visitInsn(DUP); // Duplicate the reference to the labels' array
            addBipushInsn(mv, i);
            addBipushInsn(mv, System.identityHashCode(labels[i])); // label.getOffset()
            mv.visitInsn(IASTORE);
        }
        addBipushInsn(mv, labels.length);
        mv.visitIntInsn(NEWARRAY, T_INT); // Creates a second array for case values

        for (int i = 0; i < labels.length; i++) {
            mv.visitInsn(DUP); // Duplicate the reference to the case values' array
            addBipushInsn(mv, i);
            addBipushInsn(mv, min + i); // Store the case value
            mv.visitInsn(IASTORE);
        }

        mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "TABLESWITCH", desc, false);
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        int iid3;

        String desc = "(II[I[I)V";
        addBipushInsn(mv, iid3 = instrumentationState.incAndGetId());
        if (config.isInstructionIds()) {
            desc = "(III[I[I)V";
            addBipushInsn(mv, instrumentationState.getMid());
        }
        addBipushInsn(mv, System.identityHashCode(dflt)); // label.getOffset()

        addBipushInsn(mv, keys.length);
        mv.visitIntInsn(NEWARRAY, T_INT);
        for (int i = 0; i < keys.length; i++) {
            mv.visitInsn(DUP);
            addBipushInsn(mv, i);
            addBipushInsn(mv, keys[i]);
            mv.visitInsn(IASTORE);
        }

        addBipushInsn(mv, labels.length);
        mv.visitIntInsn(NEWARRAY, T_INT);
        for (int i = 0; i < labels.length; i++) {
            if (i != 0) {
                iid3 = instrumentationState.incAndGetId();
            }

            mv.visitInsn(DUP);
            addBipushInsn(mv, i);
            addBipushInsn(mv, System.identityHashCode(labels[i])); // label.getOffset()
            mv.visitInsn(IASTORE);
        }

        mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "LOOKUPSWITCH", desc, false);
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        String d = "(ILjava/lang/String;I)V";
        addBipushInsn(mv, instrumentationState.incAndGetId());
        if (config.isInstructionIds()) {
            d = "(IILjava/lang/String;I)V";
            addBipushInsn(mv, instrumentationState.getMid());
        }
        mv.visitLdcInsn(desc);
        addBipushInsn(mv, dims);
        mv.visitMethodInsn(INVOKESTATIC, config.getAnalysisClass(), "MULTIANEWARRAY", d, false);
        mv.visitMultiANewArrayInsn(desc, dims);
        addSpecialInsn(mv, 0); // for non-exceptional path
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        for (TryCatchBlock b : tryCatchBlocks) {
            b.visit(mv);
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitTryCatchBlock(Label label, Label label1, Label label2, String s) {
        tryCatchBlocks.addLast(new TryCatchBlock(label, label1, label2, s));
    }
}
