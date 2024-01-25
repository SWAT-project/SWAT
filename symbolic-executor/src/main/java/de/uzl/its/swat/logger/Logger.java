package de.uzl.its.swat.logger;

public interface Logger {
    void LDC(int iid, int mid, int c);

    void LDC(int iid, int mid, long c);

    void LDC(int iid, int mid, float c);

    void LDC(int iid, int mid, double c);

    void LDC(int iid, int mid, String c);

    void LDC(int iid, int mid, Object c);

    void IINC(int iid, int mid, int var, int increment);

    void MULTIANEWARRAY(int iid, int mid, String desc, int dims);

    void LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels);

    void TABLESWITCH(int iid, int mid, int min, int max, int dflt, int[] labels, int[] values);

    void IFEQ(int iid, int mid, int label);

    void IFNE(int iid, int mid, int label);

    void IFLT(int iid, int mid, int label);

    void IFGE(int iid, int mid, int label);

    void IFGT(int iid, int mid, int label);

    void IFLE(int iid, int mid, int label);

    void IF_ICMPEQ(int iid, int mid, int label);

    void IF_ICMPNE(int iid, int mid, int label);

    void IF_ICMPLT(int iid, int mid, int label);

    void IF_ICMPGE(int iid, int mid, int label);

    void IF_ICMPGT(int iid, int mid, int label);

    void IF_ICMPLE(int iid, int mid, int label);

    void IF_ACMPEQ(int iid, int mid, int label);

    void IF_ACMPNE(int iid, int mid, int label);

    void GOTO(int iid, int mid, int label);

    void JSR(int iid, int mid, int label);

    void IFNULL(int iid, int mid, int label);

    void IFNONNULL(int iid, int mid, int label);

    void INVOKEVIRTUAL(int iid, int mid, String owner, String name, String desc);

    void INVOKESPECIAL(int iid, int mid, String owner, String name, String desc);

    void INVOKEDYNAMIC(int iid, int mid, String owner, String name, String desc, String lambdaFun);

    void INVOKESTATIC(int iid, int mid, String owner, String name, String desc);

    void INVOKEINTERFACE(int iid, int mid, String owner, String name, String desc);

    void GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc);

    void PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc);

    void GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc);

    void PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc);

    void NEW(int iid, int mid, String type, int cIdx);

    void ANEWARRAY(int iid, int mid, String type);

    void CHECKCAST(int iid, int mid, String type);

    void INSTANCEOF(int iid, int mid, String type);

    void BIPUSH(int iid, int mid, int value);

    void SIPUSH(int iid, int mid, int value);

    void NEWARRAY(int iid, int mid, int atype);

    void ILOAD(int iid, int mid, int var);

    void LLOAD(int iid, int mid, int var);

    void FLOAD(int iid, int mid, int var);

    void DLOAD(int iid, int mid, int var);

    void ALOAD(int iid, int mid, int var);

    void ISTORE(int iid, int mid, int var);

    void LSTORE(int iid, int mid, int var);

    void FSTORE(int iid, int mid, int var);

    void DSTORE(int iid, int mid, int var);

    void ASTORE(int iid, int mid, int var);

    void RET(int iid, int mid, int var);

    void NOP(int iid, int mid);

    void ACONST_NULL(int iid, int mid);

    void ICONST_M1(int iid, int mid);

    void ICONST_0(int iid, int mid);

    void ICONST_1(int iid, int mid);

    void ICONST_2(int iid, int mid);

    void ICONST_3(int iid, int mid);

    void ICONST_4(int iid, int mid);

    void ICONST_5(int iid, int mid);

    void LCONST_0(int iid, int mid);

    void LCONST_1(int iid, int mid);

    void FCONST_0(int iid, int mid);

    void FCONST_1(int iid, int mid);

    void FCONST_2(int iid, int mid);

    void DCONST_0(int iid, int mid);

    void DCONST_1(int iid, int mid);

    void IALOAD(int iid, int mid);

    void LALOAD(int iid, int mid);

    void FALOAD(int iid, int mid);

    void DALOAD(int iid, int mid);

    void AALOAD(int iid, int mid);

    void BALOAD(int iid, int mid);

    void CALOAD(int iid, int mid);

    void SALOAD(int iid, int mid);

    void IASTORE(int iid, int mid);

    void LASTORE(int iid, int mid);

    void FASTORE(int iid, int mid);

    void DASTORE(int iid, int mid);

    void AASTORE(int iid, int mid);

    void BASTORE(int iid, int mid);

    void CASTORE(int iid, int mid);

    void SASTORE(int iid, int mid);

    void POP(int iid, int mid);

    void POP2(int iid, int mid);

    void DUP(int iid, int mid);

    void DUP_X1(int iid, int mid);

    void DUP_X2(int iid, int mid);

    void DUP2(int iid, int mid);

    void DUP2_X1(int iid, int mid);

    void DUP2_X2(int iid, int mid);

    void SWAP(int iid, int mid);

    void IADD(int iid, int mid);

    void LADD(int iid, int mid);

    void FADD(int iid, int mid);

    void DADD(int iid, int mid);

    void ISUB(int iid, int mid);

    void LSUB(int iid, int mid);

    void FSUB(int iid, int mid);

    void DSUB(int iid, int mid);

    void IMUL(int iid, int mid);

    void LMUL(int iid, int mid);

    void FMUL(int iid, int mid);

    void DMUL(int iid, int mid);

    void IDIV(int iid, int mid);

    void LDIV(int iid, int mid);

    void FDIV(int iid, int mid);

    void DDIV(int iid, int mid);

    void IREM(int iid, int mid);

    void LREM(int iid, int mid);

    void FREM(int iid, int mid);

    void DREM(int iid, int mid);

    void INEG(int iid, int mid);

    void LNEG(int iid, int mid);

    void FNEG(int iid, int mid);

    void DNEG(int iid, int mid);

    void ISHL(int iid, int mid);

    void LSHL(int iid, int mid);

    void ISHR(int iid, int mid);

    void LSHR(int iid, int mid);

    void IUSHR(int iid, int mid);

    void LUSHR(int iid, int mid);

    void IAND(int iid, int mid);

    void LAND(int iid, int mid);

    void IOR(int iid, int mid);

    void LOR(int iid, int mid);

    void IXOR(int iid, int mid);

    void LXOR(int iid, int mid);

    void I2L(int iid, int mid);

    void I2F(int iid, int mid);

    void I2D(int iid, int mid);

    void L2I(int iid, int mid);

    void L2F(int iid, int mid);

    void L2D(int iid, int mid);

    void F2I(int iid, int mid);

    void F2L(int iid, int mid);

    void F2D(int iid, int mid);

    void D2I(int iid, int mid);

    void D2L(int iid, int mid);

    void D2F(int iid, int mid);

    void I2B(int iid, int mid);

    void I2C(int iid, int mid);

    void I2S(int iid, int mid);

    void LCMP(int iid, int mid);

    void FCMPL(int iid, int mid);

    void FCMPG(int iid, int mid);

    void DCMPL(int iid, int mid);

    void DCMPG(int iid, int mid);

    void IRETURN(int iid, int mid);

    void LRETURN(int iid, int mid);

    void FRETURN(int iid, int mid);

    void DRETURN(int iid, int mid);

    void ARETURN(int iid, int mid);

    void RETURN(int iid, int mid);

    void ARRAYLENGTH(int iid, int mid);

    void ATHROW(int iid, int mid);

    void MONITORENTER(int iid, int mid);

    void MONITOREXIT(int iid, int mid);

    void GETVALUE_double(double v, int i);

    void GETVALUE_long(long v, int i);

    void GETVALUE_Object(Object v, int i);

    void GETVALUE_boolean(boolean v, int i);

    void GETVALUE_byte(byte v, int i);

    void GETVALUE_char(char v, int i);

    void GETVALUE_float(float v, int i);

    void GETVALUE_int(int v, int i);

    void GETVALUE_short(short v, int i);

    void GETVALUE_void();

    void INVOKEMETHOD_EXCEPTION();

    void INVOKEMETHOD_END();

    void MAKE_SYMBOLIC();

    void LOOP_BEGIN(int iid);

    void LOOP_END(int iid);

    void SPECIAL(int i);

    void flush();
}
