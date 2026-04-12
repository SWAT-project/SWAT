package de.uzl.its.swat.symbolic.processor;

public class DummyInstructionProcessor implements InstructionProcessor {

    public void LDC(long iid, int c) {}

    public void LDC(long iid, long c) {}

    public void LDC(long iid, float c) {}

    public void LDC(long iid, double c) {}

    public void LDC(long iid, String c) {}

    public void LDC(long iid, Object c) {}

    public void IINC(long iid, int var, int increment) {}

    public void MULTIANEWARRAY(long iid, String desc, int dims) {}

    public void LOOKUPSWITCH(long iid, int dflt, int[] keys, int[] labels) {}

    public void TABLESWITCH(
            long iid, int min, int max, int dflt, int[] labels, int[] values) {}

    public void IFEQ(long iid, int label) {}

    public void IFNE(long iid, int label) {}

    public void IFLT(long iid, int label) {}

    public void IFGE(long iid, int label) {}

    public void IFGT(long iid, int label) {}

    public void IFLE(long iid, int label) {}

    public void IF_ICMPEQ(long iid, int label) {}

    public void IF_ICMPNE(long iid, int label) {}

    public void IF_ICMPLT(long iid, int label) {}

    public void IF_ICMPGE(long iid, int label) {}

    public void IF_ICMPGT(long iid, int label) {}

    public void IF_ICMPLE(long iid, int label) {}

    public void IF_ACMPEQ(long iid, int label) {}

    public void IF_ACMPNE(long iid, int label) {}

    public void GOTO(long iid, int label) {}

    public void JSR(long iid, int label) {}

    public void IFNULL(long iid, int label) {}

    public void IFNONNULL(long iid, int label) {}

    public void INVOKEVIRTUAL(
            long iid, long invokeId, String owner, String name, String desc) {}

    public void INVOKESPECIAL(
            long iid, long invokeId, String owner, String name, String desc) {}

    public void INVOKESTATIC(
            long iid, long invokeId, String owner, String name, String desc) {}

    public void INVOKEDYNAMIC(
            long iid,
            long invokeId,
            String owner,
            String name,
            String desc,
            String lambda) {}

    public void INVOKEINTERFACE(
            long iid, long invokeId, String owner, String name, String desc) {}

    public void GETSTATIC(long iid, int cIdx, String name, String desc) {}

    public void PUTSTATIC(long iid, int cIdx, String name, String desc) {}

    public void GETFIELD(long iid, int cIdx, String name, String desc) {}

    public void PUTFIELD(long iid, int cIdx, String name, String desc) {}

    public void NEW(long iid, String type, int cIdx) {}

    public void CLINIT(long iid, int cIdx, long invokeId) {}

    public void UNPACK_INVOKE_PARAMETER(long iid) {}

    public void SET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, boolean isWideOperand, int modifiers, long iid, long invokeId) {}

    public void GET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, int modifiers, long iid, long invokeId) {}

    public void ANEWARRAY(long iid, String type) {}

    public void CHECKCAST(long iid, String type) {}

    public void INSTANCEOF(long iid, String type) {}

    public void BIPUSH(long iid, int value) {}

    public void SIPUSH(long iid, int value) {}

    public void NEWARRAY(long iid, int atype) {}

    public void ILOAD(long iid, int var) {}

    public void LLOAD(long iid, int var) {}

    public void FLOAD(long iid, int var) {}

    public void DLOAD(long iid, int var) {}

    public void ALOAD(long iid, int var) {}

    public void ISTORE(long iid, int var) {}

    public void LSTORE(long iid, int var) {}

    public void FSTORE(long iid, int var) {}

    public void DSTORE(long iid, int var) {}

    public void ASTORE(long iid, int var) {}

    public void RET(long iid, int var) {}

    public void NOP(long iid) {}

    public void ACONST_NULL(long iid) {}

    public void ICONST_M1(long iid) {}

    public void ICONST_0(long iid) {}

    public void ICONST_1(long iid) {}

    public void ICONST_2(long iid) {}

    public void ICONST_3(long iid) {}

    public void ICONST_4(long iid) {}

    public void ICONST_5(long iid) {}

    public void LCONST_0(long iid) {}

    public void LCONST_1(long iid) {}

    public void FCONST_0(long iid) {}

    public void FCONST_1(long iid) {}

    public void FCONST_2(long iid) {}

    public void DCONST_0(long iid) {}

    public void DCONST_1(long iid) {}

    public void IALOAD(long iid) {}

    public void LALOAD(long iid) {}

    public void FALOAD(long iid) {}

    public void DALOAD(long iid) {}

    public void AALOAD(long iid) {}

    public void BALOAD(long iid) {}

    public void CALOAD(long iid) {}

    public void SALOAD(long iid) {}

    public void IASTORE(long iid) {}

    public void LASTORE(long iid) {}

    public void FASTORE(long iid) {}

    public void DASTORE(long iid) {}

    public void AASTORE(long iid) {}

    public void BASTORE(long iid) {}

    public void CASTORE(long iid) {}

    public void SASTORE(long iid) {}

    public void POP(long iid) {}

    public void POP2(long iid) {}

    public void DUP(long iid) {}

    public void DUP_X1(long iid) {}

    public void DUP_X2(long iid) {}

    public void DUP2(long iid) {}

    public void DUP2_X1(long iid) {}

    public void DUP2_X2(long iid) {}

    public void SWAP(long iid) {}

    public void IADD(long iid) {}

    public void LADD(long iid) {}

    public void FADD(long iid) {}

    public void DADD(long iid) {}

    public void ISUB(long iid) {}

    public void LSUB(long iid) {}

    public void FSUB(long iid) {}

    public void DSUB(long iid) {}

    public void IMUL(long iid) {}

    public void LMUL(long iid) {}

    public void FMUL(long iid) {}

    public void DMUL(long iid) {}

    public void IDIV(long iid) {}

    public void LDIV(long iid) {}

    public void FDIV(long iid) {}

    public void DDIV(long iid) {}

    public void IREM(long iid) {}

    public void LREM(long iid) {}

    public void FREM(long iid) {}

    public void DREM(long iid) {}

    public void INEG(long iid) {}

    public void LNEG(long iid) {}

    public void FNEG(long iid) {}

    public void DNEG(long iid) {}

    public void ISHL(long iid) {}

    public void LSHL(long iid) {}

    public void ISHR(long iid) {}

    public void LSHR(long iid) {}

    public void IUSHR(long iid) {}

    public void LUSHR(long iid) {}

    public void IAND(long iid) {}

    public void LAND(long iid) {}

    public void IOR(long iid) {}

    public void LOR(long iid) {}

    public void IXOR(long iid) {}

    public void LXOR(long iid) {}

    public void I2L(long iid) {}

    public void I2F(long iid) {}

    public void I2D(long iid) {}

    public void L2I(long iid) {}

    public void L2F(long iid) {}

    public void L2D(long iid) {}

    public void F2I(long iid) {}

    public void F2L(long iid) {}

    public void F2D(long iid) {}

    public void D2I(long iid) {}

    public void D2L(long iid) {}

    public void D2F(long iid) {}

    public void I2B(long iid) {}

    public void I2C(long iid) {}

    public void I2S(long iid) {}

    public void LCMP(long iid) {}

    public void FCMPL(long iid) {}

    public void FCMPG(long iid) {}

    public void DCMPL(long iid) {}

    public void DCMPG(long iid) {}

    public void IRETURN(long iid) {}

    public void LRETURN(long iid) {}

    public void FRETURN(long iid) {}

    public void DRETURN(long iid) {}

    public void ARETURN(long iid) {}

    public void RETURN(long iid) {}

    public void ARRAYLENGTH(long iid) {}

    public void ATHROW(long iid) {}

    public void MONITORENTER(long iid) {}

    public void MONITOREXIT(long iid) {}

    public void GETVALUE_double(long iid, double v, int i) {}

    public void GETVALUE_long(long iid, long v, int i) {}

    public void GETVALUE_Object(long iid, Object v, int i) {}

    public void GETVALUE_boolean(long iid, boolean v, int i) {}

    public void GETVALUE_byte(long iid, byte v, int i) {}

    public void GETVALUE_char(long iid, char v, int i) {}

    public void GETVALUE_float(long iid, float v, int i) {}

    public void GETVALUE_int(long iid, int v, int i) {}

    public void GETVALUE_short(long iid, short v, int i) {}

    public void GETVALUE_void(long iid) {}

    public void INVOKEMETHOD_EXCEPTION(long iid, long invokeId) {}

    public void INVOKEMETHOD_END(long iid, long invokeId) {}

    public void INVOKECLINIT_END(long iid, long invokeId) {}

    public void MAKE_SYMBOLIC(long iid) {}

    @Override
    public void LOOP_BEGIN(long iid) {}

    @Override
    public void LOOP_END(long iid) {}

    public void SPECIAL(long iid, int i) {}

    public void flush() {}
}
