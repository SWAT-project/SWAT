package de.uzl.its.swat.symbolic.processor;

/**
 * Interface for instruction handling. The implementation of each instruction is called through the
 * static API from the instrumented code.
 */
public interface InstructionProcessor {
    void LDC(long iid, int c);

    void LDC(long iid, long c);

    void LDC(long iid, float c);

    void LDC(long iid, double c);

    void LDC(long iid, String c);

    void LDC(long iid, Object c);

    void IINC(long iid, int var, int increment);

    void MULTIANEWARRAY(long iid, String desc, int dims);

    void LOOKUPSWITCH(long iid, int dflt, int[] keys, int[] labels);

    void TABLESWITCH(long iid, int min, int max, int dflt, int[] labels, int[] values);

    void IFEQ(long iid, int label);

    void IFNE(long iid, int label);

    void IFLT(long iid, int label);

    void IFGE(long iid, int label);

    void IFGT(long iid, int label);

    void IFLE(long iid, int label);

    void IF_ICMPEQ(long iid, int label);

    void IF_ICMPNE(long iid, int label);

    void IF_ICMPLT(long iid, int label);

    void IF_ICMPGE(long iid, int label);

    void IF_ICMPGT(long iid, int label);

    void IF_ICMPLE(long iid, int label);

    void IF_ACMPEQ(long iid, int label);

    void IF_ACMPNE(long iid, int label);

    void GOTO(long iid, int label);

    void JSR(long iid, int label);

    void IFNULL(long iid, int label);

    void IFNONNULL(long iid, int label);

    void INVOKEVIRTUAL(long iid, long invokeId, String owner, String name, String desc);

    void INVOKESPECIAL(long iid, long invokeId, String owner, String name, String desc);

    void INVOKEDYNAMIC(
            long iid,
            long invokeId,
            String owner,
            String name,
            String desc,
            String lambdaFun);

    void INVOKESTATIC(long iid, long invokeId, String owner, String name, String desc);

    void INVOKEINTERFACE(long iid, long invokeId, String owner, String name, String desc);

    void GETSTATIC(long iid, int cIdx, String name, String desc);

    void PUTSTATIC(long iid, int cIdx, String name, String desc);

    void GETFIELD(long iid, int cIdx, String name, String desc);

    void PUTFIELD(long iid, int cIdx, String name, String desc);

    void NEW(long iid, String type, int cIdx);

    void CLINIT(long iid, int cIdx, long invokeId);

    void UNPACK_INVOKE_PARAMETER(long iid);

    void SET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, boolean isWideOperand, int modifiers, long iid, long invokeId);

    void GET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, int modifiers, long iid, long invokeId);

    void ANEWARRAY(long iid, String type);

    void CHECKCAST(long iid, String type);

    void INSTANCEOF(long iid, String type);

    void BIPUSH(long iid, int value);

    void SIPUSH(long iid, int value);

    void NEWARRAY(long iid, int atype);

    void ILOAD(long iid, int var);

    void LLOAD(long iid, int var);

    void FLOAD(long iid, int var);

    void DLOAD(long iid, int var);

    void ALOAD(long iid, int var);

    void ISTORE(long iid, int var);

    void LSTORE(long iid, int var);

    void FSTORE(long iid, int var);

    void DSTORE(long iid, int var);

    void ASTORE(long iid, int var);

    void RET(long iid, int var);

    void NOP(long iid);

    void ACONST_NULL(long iid);

    void ICONST_M1(long iid);

    void ICONST_0(long iid);

    void ICONST_1(long iid);

    void ICONST_2(long iid);

    void ICONST_3(long iid);

    void ICONST_4(long iid);

    void ICONST_5(long iid);

    void LCONST_0(long iid);

    void LCONST_1(long iid);

    void FCONST_0(long iid);

    void FCONST_1(long iid);

    void FCONST_2(long iid);

    void DCONST_0(long iid);

    void DCONST_1(long iid);

    void IALOAD(long iid);

    void LALOAD(long iid);

    void FALOAD(long iid);

    void DALOAD(long iid);

    void AALOAD(long iid);

    void BALOAD(long iid);

    void CALOAD(long iid);

    void SALOAD(long iid);

    void IASTORE(long iid);

    void LASTORE(long iid);

    void FASTORE(long iid);

    void DASTORE(long iid);

    void AASTORE(long iid);

    void BASTORE(long iid);

    void CASTORE(long iid);

    void SASTORE(long iid);

    void POP(long iid);

    void POP2(long iid);

    void DUP(long iid);

    void DUP_X1(long iid);

    void DUP_X2(long iid);

    void DUP2(long iid);

    void DUP2_X1(long iid);

    void DUP2_X2(long iid);

    void SWAP(long iid);

    void IADD(long iid);

    void LADD(long iid);

    void FADD(long iid);

    void DADD(long iid);

    void ISUB(long iid);

    void LSUB(long iid);

    void FSUB(long iid);

    void DSUB(long iid);

    void IMUL(long iid);

    void LMUL(long iid);

    void FMUL(long iid);

    void DMUL(long iid);

    void IDIV(long iid);

    void LDIV(long iid);

    void FDIV(long iid);

    void DDIV(long iid);

    void IREM(long iid);

    void LREM(long iid);

    void FREM(long iid);

    void DREM(long iid);

    void INEG(long iid);

    void LNEG(long iid);

    void FNEG(long iid);

    void DNEG(long iid);

    void ISHL(long iid);

    void LSHL(long iid);

    void ISHR(long iid);

    void LSHR(long iid);

    void IUSHR(long iid);

    void LUSHR(long iid);

    void IAND(long iid);

    void LAND(long iid);

    void IOR(long iid);

    void LOR(long iid);

    void IXOR(long iid);

    void LXOR(long iid);

    void I2L(long iid);

    void I2F(long iid);

    void I2D(long iid);

    void L2I(long iid);

    void L2F(long iid);

    void L2D(long iid);

    void F2I(long iid);

    void F2L(long iid);

    void F2D(long iid);

    void D2I(long iid);

    void D2L(long iid);

    void D2F(long iid);

    void I2B(long iid);

    void I2C(long iid);

    void I2S(long iid);

    void LCMP(long iid);

    void FCMPL(long iid);

    void FCMPG(long iid);

    void DCMPL(long iid);

    void DCMPG(long iid);

    void IRETURN(long iid);

    void LRETURN(long iid);

    void FRETURN(long iid);

    void DRETURN(long iid);

    void ARETURN(long iid);

    void RETURN(long iid);

    void ARRAYLENGTH(long iid);

    void ATHROW(long iid);

    void MONITORENTER(long iid);

    void MONITOREXIT(long iid);

    void GETVALUE_double(long iid, double v, int i);

    void GETVALUE_long(long iid, long v, int i);

    void GETVALUE_Object(long iid, Object v, int i);

    void GETVALUE_boolean(long iid, boolean v, int i);

    void GETVALUE_byte(long iid, byte v, int i);

    void GETVALUE_char(long iid, char v, int i);

    void GETVALUE_float(long iid, float v, int i);

    void GETVALUE_int(long iid, int v, int i);

    void GETVALUE_short(long iid, short v, int i);

    void GETVALUE_void(long iid);

    void INVOKEMETHOD_EXCEPTION(long iid, long invokeId);

    void INVOKECLINIT_END(long iid, long invokeId);

    void INVOKEMETHOD_END(long iid, long invokeId);

    void MAKE_SYMBOLIC(long iid);

    void LOOP_BEGIN(long iid);

    void LOOP_END(long iid);

    void SPECIAL(long iid, int i);

    void flush();
}
