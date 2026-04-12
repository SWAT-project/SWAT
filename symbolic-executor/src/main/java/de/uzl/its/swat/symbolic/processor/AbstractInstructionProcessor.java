package de.uzl.its.swat.symbolic.processor;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.symbolic.instruction.*;


/** Template Method pattern for processing instruction processing. */
public abstract class AbstractInstructionProcessor implements InstructionProcessor {

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    protected abstract void processInstruction(Instruction insn);

    public void LDC(long iid, int c) {
        processInstruction(new LDC_int(iid, c));
    }

    public void LDC(long iid, long c) {
        processInstruction(new LDC_long(iid, c));
    }

    public void LDC(long iid, float c) {
        processInstruction(new LDC_float(iid, c));
    }

    public void LDC(long iid, double c) {
        processInstruction(new LDC_double(iid, c));
    }

    public void LDC(long iid, String c) {
        processInstruction(new LDC_String(iid, c, System.identityHashCode(c)));
    }

    public void LDC(long iid, Object c) {
        processInstruction(new LDC_Object(iid, System.identityHashCode(c)));
    }

    public void IINC(long iid, int var, int increment) {
        processInstruction(new IINC(iid, var, increment));
    }

    public void MULTIANEWARRAY(long iid, String desc, int dims) {
        processInstruction(new MULTIANEWARRAY(iid, desc, dims));
    }

    public void LOOKUPSWITCH(long iid, int dflt, int[] keys, int[] labels) {
        processInstruction(new LOOKUPSWITCH(iid, dflt, keys, labels));
    }

    public void TABLESWITCH(
            long iid, int min, int max, int dflt, int[] labels, int[] values) {
        processInstruction(new TABLESWITCH(iid, min, max, dflt, labels, values));
    }

    public void IFEQ(long iid, int label) {
        processInstruction(new IFEQ(iid, label));
    }

    public void IFNE(long iid, int label) {
        processInstruction(new IFNE(iid, label));
    }

    public void IFLT(long iid, int label) {
        processInstruction(new IFLT(iid, label));
    }

    public void IFGE(long iid, int label) {
        processInstruction(new IFGE(iid, label));
    }

    public void IFGT(long iid, int label) {
        processInstruction(new IFGT(iid, label));
    }

    public void IFLE(long iid, int label) {
        processInstruction(new IFLE(iid, label));
    }

    public void IF_ICMPEQ(long iid, int label) {
        processInstruction(new IF_ICMPEQ(iid, label));
    }

    public void IF_ICMPNE(long iid, int label) {
        processInstruction(new IF_ICMPNE(iid, label));
    }

    public void IF_ICMPLT(long iid, int label) {
        processInstruction(new IF_ICMPLT(iid, label));
    }

    public void IF_ICMPGE(long iid, int label) {
        processInstruction(new IF_ICMPGE(iid, label));
    }

    public void IF_ICMPGT(long iid, int label) {
        processInstruction(new IF_ICMPGT(iid, label));
    }

    public void IF_ICMPLE(long iid, int label) {
        processInstruction(new IF_ICMPLE(iid, label));
    }

    public void IF_ACMPEQ(long iid, int label) {
        processInstruction(new IF_ACMPEQ(iid, label));
    }

    public void IF_ACMPNE(long iid, int label) {
        processInstruction(new IF_ACMPNE(iid, label));
    }

    public void GOTO(long iid, int label) {
        processInstruction(new GOTO(iid, label));
    }

    public void JSR(long iid, int label) {
        processInstruction(new JSR(iid, label));
    }

    public void IFNULL(long iid, int label) {
        processInstruction(new IFNULL(iid, label));
    }

    public void IFNONNULL(long iid, int label) {
        processInstruction(new IFNONNULL(iid, label));
    }

    public void INVOKEVIRTUAL(
            long iid, long invokeId, String owner, String name, String desc) {
        processInstruction(new INVOKEVIRTUAL(iid, invokeId, owner, name, desc));
    }

    public void INVOKESPECIAL(
            long iid, long invokeId, String owner, String name, String desc) {
        processInstruction(new INVOKESPECIAL(iid, invokeId, owner, name, desc));
    }

    public void INVOKEDYNAMIC(
            long iid,
            long invokeId,
            String owner,
            String name,
            String desc,
            String lambda) {
        processInstruction(new INVOKEDYNAMIC(iid, invokeId, owner, name, desc, lambda));
    }

    public void INVOKESTATIC(
            long iid, long invokeId, String owner, String name, String desc) {
        processInstruction(new INVOKESTATIC(iid, invokeId, owner, name, desc));
    }

    public void INVOKEINTERFACE(
            long iid, long invokeId, String owner, String name, String desc) {
        processInstruction(new INVOKEINTERFACE(iid, invokeId, owner, name, desc));
    }

    public void GETSTATIC(long iid, int cIdx, String name, String desc) {
        processInstruction(new GETSTATIC(iid, cIdx, name, desc));
    }

    public void PUTSTATIC(long iid, int cIdx, String name, String desc) {
        processInstruction(new PUTSTATIC(iid, cIdx, name, desc));
    }

    public void GETFIELD(long iid, int cIdx, String name, String desc) {
        processInstruction(new GETFIELD(iid, cIdx, name, desc));
    }

    public void PUTFIELD(long iid, int cIdx, String name, String desc) {
        processInstruction(new PUTFIELD(iid, cIdx, name, desc));
    }

    public void NEW(long iid, String type, int cIdx) {
        processInstruction(new NEW(iid, type, cIdx));
    }

    public void CLINIT(long iid, int cIdx, long invokeId) {
        processInstruction(new CLINIT(iid, cIdx, invokeId));
    }

    public void UNPACK_INVOKE_PARAMETER(long iid) {
        processInstruction(new UNPACK_INVOKE_PARAMETER(iid));
    }

    public void SET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, boolean isWideOperand, int modifiers, long iid, long invokeId) {
        processInstruction(new SET_FIELD_REFLECTION(owner, name, desc, reflectFieldName, reflectObjectOwner, isWideOperand, modifiers, iid, invokeId));
    }

    public void GET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, int modifiers, long iid, long invokeId) {
        processInstruction(new GET_FIELD_REFLECTION(owner, name, desc, reflectFieldName, reflectObjectOwner, modifiers, iid, invokeId));
    }

    public void ANEWARRAY(long iid, String type) {
        processInstruction(new ANEWARRAY(iid, type));
    }

    public void CHECKCAST(long iid, String type) {
        processInstruction(new CHECKCAST(iid, type));
    }

    public void INSTANCEOF(long iid, String type) {
        processInstruction(new INSTANCEOF(iid, type));
    }

    public void BIPUSH(long iid, int value) {
        processInstruction(new BIPUSH(iid, value));
    }

    public void SIPUSH(long iid, int value) {
        processInstruction(new SIPUSH(iid, value));
    }

    public void NEWARRAY(long iid, int atype) {
        processInstruction(new NEWARRAY(iid, atype));
    }

    public void ILOAD(long iid, int var) {
        processInstruction(new ILOAD(iid, var));
    }

    public void LLOAD(long iid, int var) {
        processInstruction(new LLOAD(iid, var));
    }

    public void FLOAD(long iid, int var) {
        processInstruction(new FLOAD(iid, var));
    }

    public void DLOAD(long iid, int var) {
        processInstruction(new DLOAD(iid, var));
    }

    public void ALOAD(long iid, int var) {
        processInstruction(new ALOAD(iid, var));
    }

    public void ISTORE(long iid, int var) {
        processInstruction(new ISTORE(iid, var));
    }

    public void LSTORE(long iid, int var) {
        processInstruction(new LSTORE(iid, var));
    }

    public void FSTORE(long iid, int var) {
        processInstruction(new FSTORE(iid, var));
    }

    public void DSTORE(long iid, int var) {
        processInstruction(new DSTORE(iid, var));
    }

    public void ASTORE(long iid, int var) {
        processInstruction(new ASTORE(iid, var));
    }

    public void RET(long iid, int var) {
        processInstruction(new RET(iid, var));
    }

    public void NOP(long iid) {
        processInstruction(new NOP(iid));
    }

    public void ACONST_NULL(long iid) {
        processInstruction(new ACONST_NULL(iid));
    }

    public void ICONST_M1(long iid) {
        processInstruction(new ICONST_M1(iid));
    }

    public void ICONST_0(long iid) {
        processInstruction(new ICONST_0(iid));
    }

    public void ICONST_1(long iid) {
        processInstruction(new ICONST_1(iid));
    }

    public void ICONST_2(long iid) {
        processInstruction(new ICONST_2(iid));
    }

    public void ICONST_3(long iid) {
        processInstruction(new ICONST_3(iid));
    }

    public void ICONST_4(long iid) {
        processInstruction(new ICONST_4(iid));
    }

    public void ICONST_5(long iid) {
        processInstruction(new ICONST_5(iid));
    }

    public void LCONST_0(long iid) {
        processInstruction(new LCONST_0(iid));
    }

    public void LCONST_1(long iid) {
        processInstruction(new LCONST_1(iid));
    }

    public void FCONST_0(long iid) {
        processInstruction(new FCONST_0(iid));
    }

    public void FCONST_1(long iid) {
        processInstruction(new FCONST_1(iid));
    }

    public void FCONST_2(long iid) {
        processInstruction(new FCONST_2(iid));
    }

    public void DCONST_0(long iid) {
        processInstruction(new DCONST_0(iid));
    }

    public void DCONST_1(long iid) {
        processInstruction(new DCONST_1(iid));
    }

    public void IALOAD(long iid) {
        processInstruction(new IALOAD(iid));
    }

    public void LALOAD(long iid) {
        processInstruction(new LALOAD(iid));
    }

    public void FALOAD(long iid) {
        processInstruction(new FALOAD(iid));
    }

    public void DALOAD(long iid) {
        processInstruction(new DALOAD(iid));
    }

    public void AALOAD(long iid) {
        processInstruction(new AALOAD(iid));
    }

    public void BALOAD(long iid) {
        processInstruction(new BALOAD(iid));
    }

    public void CALOAD(long iid) {
        processInstruction(new CALOAD(iid));
    }

    public void SALOAD(long iid) {
        processInstruction(new SALOAD(iid));
    }

    public void IASTORE(long iid) {
        processInstruction(new IASTORE(iid));
    }

    public void LASTORE(long iid) {
        processInstruction(new LASTORE(iid));
    }

    public void FASTORE(long iid) {
        processInstruction(new FASTORE(iid));
    }

    public void DASTORE(long iid) {
        processInstruction(new DASTORE(iid));
    }

    public void AASTORE(long iid) {
        processInstruction(new AASTORE(iid));
    }

    public void BASTORE(long iid) {
        processInstruction(new BASTORE(iid));
    }

    public void CASTORE(long iid) {
        processInstruction(new CASTORE(iid));
    }

    public void SASTORE(long iid) {
        processInstruction(new SASTORE(iid));
    }

    public void POP(long iid) {
        processInstruction(new POP(iid));
    }

    public void POP2(long iid) {
        processInstruction(new POP2(iid));
    }

    public void DUP(long iid) {
        processInstruction(new DUP(iid));
    }

    public void DUP_X1(long iid) {
        processInstruction(new DUP_X1(iid));
    }

    public void DUP_X2(long iid) {
        processInstruction(new DUP_X2(iid));
    }

    public void DUP2(long iid) {
        processInstruction(new DUP2(iid));
    }

    public void DUP2_X1(long iid) {
        processInstruction(new DUP2_X1(iid));
    }

    public void DUP2_X2(long iid) {
        processInstruction(new DUP2_X2(iid));
    }

    public void SWAP(long iid) {
        processInstruction(new SWAP(iid));
    }

    public void IADD(long iid) {
        processInstruction(new IADD(iid));
    }

    public void LADD(long iid) {
        processInstruction(new LADD(iid));
    }

    public void FADD(long iid) {
        processInstruction(new FADD(iid));
    }

    public void DADD(long iid) {
        processInstruction(new DADD(iid));
    }

    public void ISUB(long iid) {
        processInstruction(new ISUB(iid));
    }

    public void LSUB(long iid) {
        processInstruction(new LSUB(iid));
    }

    public void FSUB(long iid) {
        processInstruction(new FSUB(iid));
    }

    public void DSUB(long iid) {
        processInstruction(new DSUB(iid));
    }

    public void IMUL(long iid) {
        processInstruction(new IMUL(iid));
    }

    public void LMUL(long iid) {
        processInstruction(new LMUL(iid));
    }

    public void FMUL(long iid) {
        processInstruction(new FMUL(iid));
    }

    public void DMUL(long iid) {
        processInstruction(new DMUL(iid));
    }

    public void IDIV(long iid) {
        processInstruction(new IDIV(iid));
    }

    public void LDIV(long iid) {
        processInstruction(new LDIV(iid));
    }

    public void FDIV(long iid) {
        processInstruction(new FDIV(iid));
    }

    public void DDIV(long iid) {
        processInstruction(new DDIV(iid));
    }

    public void IREM(long iid) {
        processInstruction(new IREM(iid));
    }

    public void LREM(long iid) {
        processInstruction(new LREM(iid));
    }

    public void FREM(long iid) {
        processInstruction(new FREM(iid));
    }

    public void DREM(long iid) {
        processInstruction(new DREM(iid));
    }

    public void INEG(long iid) {
        processInstruction(new INEG(iid));
    }

    public void LNEG(long iid) {
        processInstruction(new LNEG(iid));
    }

    public void FNEG(long iid) {
        processInstruction(new FNEG(iid));
    }

    public void DNEG(long iid) {
        processInstruction(new DNEG(iid));
    }

    public void ISHL(long iid) {
        processInstruction(new ISHL(iid));
    }

    public void LSHL(long iid) {
        processInstruction(new LSHL(iid));
    }

    public void ISHR(long iid) {
        processInstruction(new ISHR(iid));
    }

    public void LSHR(long iid) {
        processInstruction(new LSHR(iid));
    }

    public void IUSHR(long iid) {
        processInstruction(new IUSHR(iid));
    }

    public void LUSHR(long iid) {
        processInstruction(new LUSHR(iid));
    }

    public void IAND(long iid) {
        processInstruction(new IAND(iid));
    }

    public void LAND(long iid) {
        processInstruction(new LAND(iid));
    }

    public void IOR(long iid) {
        processInstruction(new IOR(iid));
    }

    public void LOR(long iid) {
        processInstruction(new LOR(iid));
    }

    public void IXOR(long iid) {
        processInstruction(new IXOR(iid));
    }

    public void LXOR(long iid) {
        processInstruction(new LXOR(iid));
    }

    public void I2L(long iid) {
        processInstruction(new I2L(iid));
    }

    public void I2F(long iid) {
        processInstruction(new I2F(iid));
    }

    public void I2D(long iid) {
        processInstruction(new I2D(iid));
    }

    public void L2I(long iid) {
        processInstruction(new L2I(iid));
    }

    public void L2F(long iid) {
        processInstruction(new L2F(iid));
    }

    public void L2D(long iid) {
        processInstruction(new L2D(iid));
    }

    public void F2I(long iid) {
        processInstruction(new F2I(iid));
    }

    public void F2L(long iid) {
        processInstruction(new F2L(iid));
    }

    public void F2D(long iid) {
        processInstruction(new F2D(iid));
    }

    public void D2I(long iid) {
        processInstruction(new D2I(iid));
    }

    public void D2L(long iid) {
        processInstruction(new D2L(iid));
    }

    public void D2F(long iid) {
        processInstruction(new D2F(iid));
    }

    public void I2B(long iid) {
        processInstruction(new I2B(iid));
    }

    public void I2C(long iid) {
        processInstruction(new I2C(iid));
    }

    public void I2S(long iid) {
        processInstruction(new I2S(iid));
    }

    public void LCMP(long iid) {
        processInstruction(new LCMP(iid));
    }

    public void FCMPL(long iid) {
        processInstruction(new FCMPL(iid));
    }

    public void FCMPG(long iid) {
        processInstruction(new FCMPG(iid));
    }

    public void DCMPL(long iid) {
        processInstruction(new DCMPL(iid));
    }

    public void DCMPG(long iid) {
        processInstruction(new DCMPG(iid));
    }

    public void IRETURN(long iid) {
        processInstruction(new IRETURN(iid));
    }

    public void LRETURN(long iid) {
        processInstruction(new LRETURN(iid));
    }

    public void FRETURN(long iid) {
        processInstruction(new FRETURN(iid));
    }

    public void DRETURN(long iid) {
        processInstruction(new DRETURN(iid));
    }

    public void ARETURN(long iid) {
        processInstruction(new ARETURN(iid));
    }

    public void RETURN(long iid) {
        processInstruction(new RETURN(iid));
    }

    public void ARRAYLENGTH(long iid) {
        processInstruction(new ARRAYLENGTH(iid));
    }

    public void ATHROW(long iid) {
        processInstruction(new ATHROW(iid));
    }

    public void MONITORENTER(long iid) {
        processInstruction(new MONITORENTER(iid));
    }

    public void MONITOREXIT(long iid) {
        processInstruction(new MONITOREXIT(iid));
    }

    public void GETVALUE_double(long iid, double v, int i) {
        processInstruction(new GETVALUE_double(iid, v, i));
    }

    public void GETVALUE_long(long iid, long v, int i) {
        processInstruction(new GETVALUE_long(iid, v, i));
    }

    public void GETVALUE_Object(long iid, Object v, int i) {
        // ToDo: Add all supported objects
        int id = System.identityHashCode(v);
        if (v == null) {
            processInstruction(new GETVALUE_Object<>(iid, id, null, i));
        } else if (v instanceof String val) {
            processInstruction(new GETVALUE_Object<>(iid, id, val, i));
        } else if (v instanceof Long val) {
            processInstruction(new GETVALUE_Object<>(iid, id, val, i));
        } else if (v instanceof Integer val) {
            processInstruction(new GETVALUE_Object<>(iid, id, val, i));
        } else if (v instanceof Short val) {
            processInstruction(new GETVALUE_Object<>(iid, id, val, i));
        } else if (v instanceof Double val) {
            processInstruction(new GETVALUE_Object<>(iid, id, val, i));
        } else if (v instanceof Float val) {
            processInstruction(new GETVALUE_Object<>(iid, id, val, i));
        } else if (v instanceof Boolean val) {
            processInstruction(new GETVALUE_Object<>(iid, id, val, i));
        } else {
            processInstruction(new GETVALUE_Object<>(iid, id, v, i));
        }
    }

    public void GETVALUE_boolean(long iid, boolean v, int i) {
        processInstruction(new GETVALUE_boolean(iid, v, i));
    }

    public void GETVALUE_byte(long iid, byte v, int i) {
        processInstruction(new GETVALUE_byte(iid, v, i));
    }

    public void GETVALUE_char(long iid, char v, int i) {
        processInstruction(new GETVALUE_char(iid, v, i));
    }

    public void GETVALUE_float(long iid, float v, int i) {
        processInstruction(new GETVALUE_float(iid, v, i));
    }

    public void GETVALUE_int(long iid, int v, int i) {
        processInstruction(new GETVALUE_int(iid, v, i));
    }

    public void GETVALUE_short(long iid, short v, int i) {
        processInstruction(new GETVALUE_short(iid, v, i));
    }

    public void GETVALUE_void(long iid) {
        processInstruction(new GETVALUE_void(iid));
    }

    public void INVOKEMETHOD_EXCEPTION(long iid, long invokeId) {
        processInstruction(new INVOKEMETHOD_EXCEPTION(iid, invokeId));
    }

    public void INVOKEMETHOD_END(long iid, long invokeId) {
        processInstruction(new INVOKEMETHOD_END(iid, invokeId));
    }

    public void INVOKECLINIT_END(long iid, long invokeId) {
        processInstruction(new INVOKECLINIT_END(iid, invokeId));
    }

    public void MAKE_SYMBOLIC(long iid) {
        processInstruction(new MAKE_SYMBOLIC(iid));
    }

    public void LOOP_BEGIN(long iid) {
        processInstruction(new LOOP_BEGIN(iid));
    }

    public void LOOP_END(long iid) {
        processInstruction(new LOOP_END(iid));
    }

    public void SPECIAL(long iid, int i) {
        processInstruction(new SPECIAL(iid, i));
    }

    public void flush() {
        processInstruction(null);
    }
}
