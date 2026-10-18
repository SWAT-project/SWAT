package de.uzl.its.swat.staticcheck;

import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

/**
 * Computes {@link AbsValue}s for one method, using the facts the check has collected so far for
 * fields, parameters and the results of calls.
 */
final class FactInterpreter extends Interpreter<AbsValue> implements Opcodes {

    /** Facts about the rest of the program, as far as they are known. */
    interface Facts {
        AbsValue field(FieldInsnNode insn);

        /** The value of parameter number {@code index} (not counting the receiver) of the method. */
        AbsValue parameter(Program.Method method, int index, Type type);

        AbsValue callResult(MethodInsnNode insn, List<? extends AbsValue> args);
    }

    private final Facts facts;
    private final Program.Method method;
    private final int[] localToParameter;

    FactInterpreter(Facts facts, Program.Method method) {
        super(ASM9);
        this.facts = facts;
        this.method = method;
        Type[] params = Type.getArgumentTypes(method.node.desc);
        int locals = method.isStatic() ? 0 : 1;
        for (Type t : params) locals += t.getSize();
        localToParameter = new int[locals];
        java.util.Arrays.fill(localToParameter, -1);
        int local = method.isStatic() ? 0 : 1;
        for (int i = 0; i < params.length; i++) {
            localToParameter[local] = i;
            local += params[i].getSize();
        }
    }

    static AbsValue ofType(Type type) {
        if (type == null) return AbsValue.UNKNOWN1;
        switch (type.getSort()) {
            case Type.VOID:
                return null;
            case Type.LONG:
            case Type.DOUBLE:
                return AbsValue.PRIM2;
            case Type.OBJECT:
            case Type.ARRAY:
                return AbsValue.ref(type.getInternalName());
            case Type.METHOD:
                return AbsValue.UNKNOWN1;
            default:
                return AbsValue.PRIM1;
        }
    }

    @Override
    public AbsValue newValue(Type type) {
        return ofType(type);
    }

    @Override
    public AbsValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
        if (isInstanceMethod && local == 0) return AbsValue.nonNull(type.getInternalName()).withThis();
        int index = local < localToParameter.length ? localToParameter[local] : -1;
        if (index < 0) return ofType(type);
        return facts.parameter(method, index, type);
    }

    @Override
    public AbsValue newEmptyValue(int local) {
        return AbsValue.UNKNOWN1;
    }

    @Override
    public AbsValue newExceptionValue(TryCatchBlockNode tryCatch, Frame<AbsValue> handlerFrame, Type exceptionType) {
        return AbsValue.caught(exceptionType.getInternalName(), method.node.tryCatchBlocks.indexOf(tryCatch));
    }

    @Override
    public AbsValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case ACONST_NULL:
                return AbsValue.NULL;
            case ICONST_M1: case ICONST_0: case ICONST_1: case ICONST_2: case ICONST_3: case ICONST_4: case ICONST_5:
                return AbsValue.constant(1, insn.getOpcode() - ICONST_0);
            case LCONST_0: case LCONST_1:
                return AbsValue.constant(2, insn.getOpcode() - LCONST_0);
            case FCONST_0: case FCONST_1: case FCONST_2:
                return AbsValue.PRIM1;
            case DCONST_0: case DCONST_1:
                return AbsValue.PRIM2;
            case BIPUSH: case SIPUSH:
                return AbsValue.constant(1, ((IntInsnNode) insn).operand);
            case LDC: {
                Object c = ((LdcInsnNode) insn).cst;
                if (c instanceof Integer) return AbsValue.constant(1, (Integer) c);
                if (c instanceof Long) return AbsValue.constant(2, (Long) c);
                if (c instanceof Float) return AbsValue.PRIM1;
                if (c instanceof Double) return AbsValue.PRIM2;
                if (c instanceof String) return AbsValue.exact("java/lang/String");
                if (c instanceof Type) {
                    int sort = ((Type) c).getSort();
                    if (sort == Type.METHOD) return AbsValue.exact("java/lang/invoke/MethodType");
                    return AbsValue.exact("java/lang/Class");
                }
                if (c instanceof Handle) return AbsValue.nonNull("java/lang/invoke/MethodHandle");
                return AbsValue.UNKNOWN1;
            }
            case JSR:
                return AbsValue.UNKNOWN1;
            case GETSTATIC:
                return facts.field((FieldInsnNode) insn);
            case NEW:
                return AbsValue.exact(((TypeInsnNode) insn).desc);
            default:
                throw new AnalyzerException(insn, "Unexpected opcode");
        }
    }

    @Override
    public AbsValue copyOperation(AbstractInsnNode insn, AbsValue value) {
        return value;
    }

    @Override
    public AbsValue unaryOperation(AbstractInsnNode insn, AbsValue value) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case INEG:
                return value.constant != null ? AbsValue.constant(1, (int) -value.constant) : AbsValue.PRIM1;
            case IINC:
                return value.constant != null
                        ? AbsValue.constant(1, (int) (value.constant + ((IincInsnNode) insn).incr)) : AbsValue.PRIM1;
            case L2I:
                return value.constant != null ? AbsValue.constant(1, (int) (long) value.constant) : AbsValue.PRIM1;
            case I2L:
                return value.constant != null ? AbsValue.constant(2, value.constant) : AbsValue.PRIM2;
            case I2B:
                return value.constant != null ? AbsValue.constant(1, (byte) (long) value.constant) : AbsValue.PRIM1;
            case I2C:
                return value.constant != null ? AbsValue.constant(1, (char) (long) value.constant) : AbsValue.PRIM1;
            case I2S:
                return value.constant != null ? AbsValue.constant(1, (short) (long) value.constant) : AbsValue.PRIM1;
            case LNEG:
                return value.constant != null ? AbsValue.constant(2, -value.constant) : AbsValue.PRIM2;
            case FNEG: case I2F: case L2F: case D2F: case F2I: case D2I:
                return AbsValue.PRIM1;
            case DNEG: case I2D: case L2D: case F2D: case F2L: case D2L:
                return AbsValue.PRIM2;
            case IFEQ: case IFNE: case IFLT: case IFGE: case IFGT: case IFLE:
            case TABLESWITCH: case LOOKUPSWITCH:
            case IRETURN: case LRETURN: case FRETURN: case DRETURN: case ARETURN:
            case PUTSTATIC: case ATHROW: case MONITORENTER: case MONITOREXIT: case IFNULL: case IFNONNULL:
                return null;
            case GETFIELD:
                return facts.field((FieldInsnNode) insn);
            case NEWARRAY: {
                String desc = "[" + primitiveDescriptor(((IntInsnNode) insn).operand);
                return AbsValue.array(desc, nonNegative(value), value.lengthOf);
            }
            case ANEWARRAY: {
                String elem = ((TypeInsnNode) insn).desc;
                String desc = "[" + (elem.startsWith("[") ? elem : "L" + elem + ";");
                return AbsValue.array(desc, nonNegative(value), value.lengthOf);
            }
            case ARRAYLENGTH: {
                AbsValue len = value.length != null ? AbsValue.constant(1, value.length) : AbsValue.PRIM1;
                return value.lengthOf != null ? len.withLengthOf(value.lengthOf) : len;
            }
            case CHECKCAST:
                return value;
            case INSTANCEOF:
                return AbsValue.PRIM1;
            default:
                throw new AnalyzerException(insn, "Unexpected opcode");
        }
    }

    private static Integer nonNegative(AbsValue count) {
        if (count.constant == null || count.constant < 0 || count.constant > Integer.MAX_VALUE) return null;
        return (int) (long) count.constant;
    }

    static String primitiveDescriptor(int operand) {
        switch (operand) {
            case T_BOOLEAN: return "Z";
            case T_CHAR: return "C";
            case T_BYTE: return "B";
            case T_SHORT: return "S";
            case T_INT: return "I";
            case T_FLOAT: return "F";
            case T_DOUBLE: return "D";
            case T_LONG: return "J";
            default: return "?";
        }
    }

    @Override
    public AbsValue binaryOperation(AbstractInsnNode insn, AbsValue v1, AbsValue v2) throws AnalyzerException {
        int op = insn.getOpcode();
        switch (op) {
            case IALOAD: case BALOAD: case CALOAD: case SALOAD: case FALOAD:
                return AbsValue.PRIM1;
            case LALOAD: case DALOAD:
                return AbsValue.PRIM2;
            case AALOAD: {
                String t = v1.staticType;
                if (t != null && t.startsWith("[")) return AbsValue.ref(Type.getType(t.substring(1)).getInternalName());
                return AbsValue.ref(null);
            }
            case IADD: case ISUB: case IMUL: case ISHL: case ISHR: case IUSHR: case IAND: case IOR: case IXOR:
                if (v1.constant != null && v2.constant != null) {
                    int a = (int) (long) v1.constant, b = (int) (long) v2.constant;
                    switch (op) {
                        case IADD: return AbsValue.constant(1, a + b);
                        case ISUB: return AbsValue.constant(1, a - b);
                        case IMUL: return AbsValue.constant(1, a * b);
                        case ISHL: return AbsValue.constant(1, a << b);
                        case ISHR: return AbsValue.constant(1, a >> b);
                        case IUSHR: return AbsValue.constant(1, a >>> b);
                        case IAND: return AbsValue.constant(1, a & b);
                        case IOR: return AbsValue.constant(1, a | b);
                        default: return AbsValue.constant(1, a ^ b);
                    }
                }
                return AbsValue.PRIM1;
            case IDIV: case IREM: case FADD: case FSUB: case FMUL: case FDIV: case FREM:
            case LCMP: case FCMPL: case FCMPG: case DCMPL: case DCMPG:
                return AbsValue.PRIM1;
            case LADD: case LSUB: case LMUL: case LDIV: case LREM: case LSHL: case LSHR: case LUSHR:
            case LAND: case LOR: case LXOR: case DADD: case DSUB: case DMUL: case DDIV: case DREM:
                return AbsValue.PRIM2;
            case IF_ICMPEQ: case IF_ICMPNE: case IF_ICMPLT: case IF_ICMPGE: case IF_ICMPGT: case IF_ICMPLE:
            case IF_ACMPEQ: case IF_ACMPNE: case PUTFIELD:
                return null;
            default:
                throw new AnalyzerException(insn, "Unexpected opcode");
        }
    }

    @Override
    public AbsValue ternaryOperation(AbstractInsnNode insn, AbsValue v1, AbsValue v2, AbsValue v3) {
        return null;
    }

    @Override
    public AbsValue naryOperation(AbstractInsnNode insn, List<? extends AbsValue> values) {
        int op = insn.getOpcode();
        if (op == MULTIANEWARRAY) {
            String desc = ((MultiANewArrayInsnNode) insn).desc;
            return AbsValue.array(desc, nonNegative(values.get(0)), values.get(0).lengthOf);
        }
        if (op == INVOKEDYNAMIC) {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
            if (ExceptionFreedomCheck.isStringConcat(indy)) return AbsValue.exact("java/lang/String");
            return ofType(Type.getReturnType(indy.desc));
        }
        return facts.callResult((MethodInsnNode) insn, values);
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, AbsValue value, AbsValue expected) {}

    @Override
    public AbsValue merge(AbsValue a, AbsValue b) {
        return AbsValue.join(a, b);
    }
}
