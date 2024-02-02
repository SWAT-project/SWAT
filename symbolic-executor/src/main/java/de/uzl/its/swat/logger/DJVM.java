package de.uzl.its.swat.logger;

import de.uzl.its.swat.thread.ThreadHandler;

public final class DJVM {

    private DJVM() {}

    public static void LDC(int iid, int c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, int c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, mid, c);
    }

    public static void LDC(int iid, long c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, long c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, mid, c);
    }

    public static void LDC(int iid, float c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, float c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, mid, c);
    }

    public static void LDC(int iid, double c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, double c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, mid, c);
    }

    public static void LDC(int iid, String c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, String c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, mid, c);
    }

    public static void LDC(int iid, Object c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, Object c) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDC(iid, mid, c);
    }

    public static void IINC(int var, int increment) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IINC(-1, -1, var, increment);
    }

    public static void IINC(int iid, int mid, int var, int increment) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IINC(iid, mid, var, increment);
    }

    public static void MULTIANEWARRAY(int iid, String desc, int dims) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.MULTIANEWARRAY(iid, -1, desc, dims);
    }

    public static void MULTIANEWARRAY(int iid, int mid, String desc, int dims) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.MULTIANEWARRAY(iid, mid, desc, dims);
    }

    public static void LOOKUPSWITCH(int iid, int dflt, int[] keys, int[] labels) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LOOKUPSWITCH(iid, -1, dflt, keys, labels);
    }

    public static void LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LOOKUPSWITCH(iid, mid, dflt, keys, labels);
    }

    public static void TABLESWITCH(
            int iid, int min, int max, int dflt, int[] labels, int[] values) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.TABLESWITCH(iid, -1, min, max, dflt, labels, values);
    }

    public static void TABLESWITCH(
            int iid, int mid, int min, int max, int dflt, int[] labels, int[] values) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.TABLESWITCH(iid, -1, min, max, dflt, labels, values);
    }

    public static void IFEQ(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFEQ(iid, -1, label);
    }

    public static void IFEQ(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFEQ(iid, mid, label);
    }

    public static void IFNE(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFNE(iid, -1, label);
    }

    public static void IFNE(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFNE(iid, mid, label);
    }

    public static void IFLT(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFLT(iid, -1, label);
    }

    public static void IFLT(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFLT(iid, mid, label);
    }

    public static void IFGE(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFGE(iid, -1, label);
    }

    public static void IFGE(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFGE(iid, mid, label);
    }

    public static void IFGT(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFGT(iid, -1, label);
    }

    public static void IFGT(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFGT(iid, mid, label);
    }

    public static void IFLE(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFLE(iid, -1, label);
    }

    public static void IFLE(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFLE(iid, mid, label);
    }

    public static void IF_ICMPEQ(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPEQ(iid, -1, label);
    }

    public static void IF_ICMPEQ(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPEQ(iid, mid, label);
    }

    public static void IF_ICMPNE(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPNE(iid, -1, label);
    }

    public static void IF_ICMPNE(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPNE(iid, mid, label);
    }

    public static void IF_ICMPLT(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPLT(iid, -1, label);
    }

    public static void IF_ICMPLT(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPLT(iid, mid, label);
    }

    public static void IF_ICMPGE(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPGE(iid, -1, label);
    }

    public static void IF_ICMPGE(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPGE(iid, mid, label);
    }

    public static void IF_ICMPGT(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPGT(iid, -1, label);
    }

    public static void IF_ICMPGT(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPGT(iid, mid, label);
    }

    public static void IF_ICMPLE(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPLE(iid, -1, label);
    }

    public static void IF_ICMPLE(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ICMPLE(iid, mid, label);
    }

    public static void IF_ACMPEQ(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ACMPEQ(iid, -1, label);
    }

    public static void IF_ACMPEQ(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ACMPEQ(iid, mid, label);
    }

    public static void IF_ACMPNE(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ACMPNE(iid, -1, label);
    }

    public static void IF_ACMPNE(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IF_ACMPNE(iid, mid, label);
    }

    public static void GOTO(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GOTO(iid, -1, label);
    }

    public static void GOTO(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GOTO(iid, mid, label);
    }

    public static void JSR(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.JSR(iid, -1, label);
    }

    public static void JSR(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.JSR(iid, mid, label);
    }

    public static void IFNULL(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFNULL(iid, -1, label);
    }

    public static void IFNULL(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFNULL(iid, mid, label);
    }

    public static void IFNONNULL(int iid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFNONNULL(iid, -1, label);
    }

    public static void IFNONNULL(int iid, int mid, int label) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IFNONNULL(iid, mid, label);
    }

    public static void INVOKEVIRTUAL(int iid, String owner, String name, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKEVIRTUAL(iid, -1, owner, name, desc);
    }

    public static void INVOKEVIRTUAL(int iid, int mid, String owner, String name, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKEVIRTUAL(iid, mid, owner, name, desc);
    }

    public static void INVOKESPECIAL(int iid, String owner, String name, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKESPECIAL(iid, -1, owner, name, desc);
    }

    public static void INVOKESPECIAL(int iid, int mid, String owner, String name, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKESPECIAL(iid, mid, owner, name, desc);
    }

    public static void INVOKEDYNAMIC(
            int iid, String owner, String name, String desc, String lambda) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKEDYNAMIC(iid, -1, owner, name, desc, lambda);
    }

    public static void INVOKEDYNAMIC(
            int iid, int mid, String owner, String name, String desc, String lambda) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKEDYNAMIC(iid, mid, owner, name, desc, lambda);
    }

    public static void INVOKESTATIC(int iid, String owner, String name, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKESTATIC(iid, -1, owner, name, desc);
    }

    public static void INVOKESTATIC(int iid, int mid, String owner, String name, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKESTATIC(iid, mid, owner, name, desc);
    }

    public static void INVOKEINTERFACE(int iid, String owner, String name, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKEINTERFACE(iid, -1, owner, name, desc);
    }

    public static void INVOKEINTERFACE(int iid, int mid, String owner, String name, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKEINTERFACE(iid, mid, owner, name, desc);
    }

    public static void GETSTATIC(int iid, int cIdx, int fIdx, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETSTATIC(iid, -1, cIdx, fIdx, desc);
    }

    public static void GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETSTATIC(iid, mid, cIdx, fIdx, desc);
    }

    public static void PUTSTATIC(int iid, int cIdx, int fIdx, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.PUTSTATIC(iid, -1, cIdx, fIdx, desc);
    }

    public static void PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.PUTSTATIC(iid, mid, cIdx, fIdx, desc);
    }

    public static void GETFIELD(int iid, int cIdx, int fIdx, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETFIELD(iid, -1, cIdx, fIdx, desc);
    }

    public static void GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETFIELD(iid, mid, cIdx, fIdx, desc);
    }

    public static void PUTFIELD(int iid, int cIdx, int fIdx, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.PUTFIELD(iid, -1, cIdx, fIdx, desc);
    }

    public static void PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.PUTFIELD(iid, mid, cIdx, fIdx, desc);
    }

    public static void NEW(int iid, String type, int cIdx) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.NEW(iid, -1, type, cIdx);
    }

    public static void NEW(int iid, int mid, String type, int cIdx) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.NEW(iid, mid, type, cIdx);
    }

    public static void ANEWARRAY(int iid, String type) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ANEWARRAY(iid, -1, type);
    }

    public static void ANEWARRAY(int iid, int mid, String type) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ANEWARRAY(iid, mid, type);
    }

    public static void CHECKCAST(int iid, String type) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.CHECKCAST(iid, -1, type);
    }

    public static void CHECKCAST(int iid, int mid, String type) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.CHECKCAST(iid, mid, type);
    }

    public static void INSTANCEOF(int iid, String type) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INSTANCEOF(iid, -1, type);
    }

    public static void INSTANCEOF(int iid, int mid, String type) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INSTANCEOF(iid, mid, type);
    }

    public static void BIPUSH(int value) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.BIPUSH(-1, -1, value);
    }

    public static void BIPUSH(int iid, int mid, int value) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.BIPUSH(iid, mid, value);
    }

    public static void SIPUSH(int value) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.SIPUSH(-1, -1, value);
    }

    public static void SIPUSH(int iid, int mid, int value) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.SIPUSH(iid, mid, value);
    }

    public static void NEWARRAY(int iid, int atype) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.NEWARRAY(iid, -1, atype);
    }

    public static void NEWARRAY(int iid, int mid, int atype) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.NEWARRAY(iid, mid, atype);
    }

    public static void ILOAD(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ILOAD(-1, -1, var);
    }

    public static void ILOAD(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ILOAD(iid, mid, var);
    }

    public static void LLOAD(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LLOAD(-1, -1, var);
    }

    public static void LLOAD(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LLOAD(iid, mid, var);
    }

    public static void FLOAD(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FLOAD(-1, -1, var);
    }

    public static void FLOAD(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FLOAD(iid, mid, var);
    }

    public static void DLOAD(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DLOAD(-1, -1, var);
    }

    public static void DLOAD(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DLOAD(iid, mid, var);
    }

    public static void ALOAD(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ALOAD(-1, -1, var);
    }

    public static void ALOAD(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ALOAD(iid, mid, var);
    }

    public static void ISTORE(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ISTORE(-1, -1, var);
    }

    public static void ISTORE(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ISTORE(iid, mid, var);
    }

    public static void LSTORE(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LSTORE(-1, -1, var);
    }

    public static void LSTORE(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LSTORE(iid, mid, var);
    }

    public static void FSTORE(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FSTORE(-1, -1, var);
    }

    public static void FSTORE(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FSTORE(iid, mid, var);
    }

    public static void DSTORE(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DSTORE(-1, -1, var);
    }

    public static void DSTORE(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DSTORE(iid, mid, var);
    }

    public static void ASTORE(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ASTORE(-1, -1, var);
    }

    public static void ASTORE(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ASTORE(iid, mid, var);
    }

    public static void RET(int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.RET(-1, -1, var);
    }

    public static void RET(int iid, int mid, int var) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.RET(iid, mid, var);
    }

    public static void NOP() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.NOP(-1, -1);
    }

    public static void NOP(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.NOP(iid, mid);
    }

    public static void ACONST_NULL() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ACONST_NULL(-1, -1);
    }

    public static void ACONST_NULL(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ACONST_NULL(iid, mid);
    }

    public static void ICONST_M1() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_M1(-1, -1);
    }

    public static void ICONST_M1(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_M1(iid, mid);
    }

    public static void ICONST_0() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_0(-1, -1);
    }

    public static void ICONST_0(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_0(iid, mid);
    }

    public static void ICONST_1() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_1(-1, -1);
    }

    public static void ICONST_1(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_1(iid, mid);
    }

    public static void ICONST_2() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_2(-1, -1);
    }

    public static void ICONST_2(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_2(iid, mid);
    }

    public static void ICONST_3() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_3(-1, -1);
    }

    public static void ICONST_3(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_3(iid, mid);
    }

    public static void ICONST_4() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_4(-1, -1);
    }

    public static void ICONST_4(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_4(iid, mid);
    }

    public static void ICONST_5() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_5(-1, -1);
    }

    public static void ICONST_5(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ICONST_5(iid, mid);
    }

    public static void LCONST_0() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LCONST_0(-1, -1);
    }

    public static void LCONST_0(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LCONST_0(iid, mid);
    }

    public static void LCONST_1() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LCONST_1(-1, -1);
    }

    public static void LCONST_1(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LCONST_1(iid, mid);
    }

    public static void FCONST_0() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCONST_0(-1, -1);
    }

    public static void FCONST_0(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCONST_0(iid, mid);
    }

    public static void FCONST_1() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCONST_1(-1, -1);
    }

    public static void FCONST_1(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCONST_1(iid, mid);
    }

    public static void FCONST_2() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCONST_2(-1, -1);
    }

    public static void FCONST_2(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCONST_2(iid, mid);
    }

    public static void DCONST_0() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DCONST_0(-1, -1);
    }

    public static void DCONST_0(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DCONST_0(iid, mid);
    }

    public static void DCONST_1() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DCONST_1(-1, -1);
    }

    public static void DCONST_1(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DCONST_1(iid, mid);
    }

    public static void IALOAD(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IALOAD(iid, -1);
    }

    public static void IALOAD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IALOAD(iid, mid);
    }

    public static void LALOAD(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LALOAD(iid, -1);
    }

    public static void LALOAD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LALOAD(iid, mid);
    }

    public static void FALOAD(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FALOAD(iid, -1);
    }

    public static void FALOAD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FALOAD(iid, mid);
    }

    public static void DALOAD(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DALOAD(iid, -1);
    }

    public static void DALOAD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DALOAD(iid, mid);
    }

    public static void AALOAD(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.AALOAD(iid, -1);
    }

    public static void AALOAD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.AALOAD(iid, mid);
    }

    public static void BALOAD(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.BALOAD(iid, -1);
    }

    public static void BALOAD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.BALOAD(iid, mid);
    }

    public static void CALOAD(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.CALOAD(iid, -1);
    }

    public static void CALOAD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.CALOAD(iid, mid);
    }

    public static void SALOAD(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.SALOAD(iid, -1);
    }

    public static void SALOAD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.SALOAD(iid, mid);
    }

    public static void IASTORE(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IASTORE(iid, -1);
    }

    public static void IASTORE(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IASTORE(iid, mid);
    }

    public static void LASTORE(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LASTORE(iid, -1);
    }

    public static void LASTORE(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LASTORE(iid, mid);
    }

    public static void FASTORE(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FASTORE(iid, -1);
    }

    public static void FASTORE(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FASTORE(iid, mid);
    }

    public static void DASTORE(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DASTORE(iid, -1);
    }

    public static void DASTORE(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DASTORE(iid, mid);
    }

    public static void AASTORE(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.AASTORE(iid, -1);
    }

    public static void AASTORE(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.AASTORE(iid, mid);
    }

    public static void BASTORE(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.BASTORE(iid, -1);
    }

    public static void BASTORE(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.BASTORE(iid, mid);
    }

    public static void CASTORE(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.CASTORE(iid, -1);
    }

    public static void CASTORE(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.CASTORE(iid, mid);
    }

    public static void SASTORE(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.SASTORE(iid, -1);
    }

    public static void SASTORE(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.SASTORE(iid, mid);
    }

    public static void POP() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.POP(-1, -1);
    }

    public static void POP(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.POP(iid, mid);
    }

    public static void POP2() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.POP2(-1, -1);
    }

    public static void POP2(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.POP2(iid, mid);
    }

    public static void DUP() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP(-1, -1);
    }

    public static void DUP(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP(iid, mid);
    }

    public static void DUP_X1() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP_X1(-1, -1);
    }

    public static void DUP_X1(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP_X1(iid, mid);
    }

    public static void DUP_X2() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP_X2(-1, -1);
    }

    public static void DUP_X2(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP_X2(iid, mid);
    }

    public static void DUP2() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP2(-1, -1);
    }

    public static void DUP2(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP2(iid, mid);
    }

    public static void DUP2_X1() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP2_X1(-1, -1);
    }

    public static void DUP2_X1(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP2_X1(iid, mid);
    }

    public static void DUP2_X2() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP2_X2(-1, -1);
    }

    public static void DUP2_X2(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DUP2_X2(iid, mid);
    }

    public static void SWAP() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.SWAP(-1, -1);
    }

    public static void SWAP(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.SWAP(iid, mid);
    }

    public static void IADD() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IADD(-1, -1);
    }

    public static void IADD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IADD(iid, mid);
    }

    public static void LADD() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LADD(-1, -1);
    }

    public static void LADD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LADD(iid, mid);
    }

    public static void FADD() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FADD(-1, -1);
    }

    public static void FADD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FADD(iid, mid);
    }

    public static void DADD() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DADD(-1, -1);
    }

    public static void DADD(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DADD(iid, mid);
    }

    public static void ISUB() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ISUB(-1, -1);
    }

    public static void ISUB(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ISUB(iid, mid);
    }

    public static void LSUB() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LSUB(-1, -1);
    }

    public static void LSUB(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LSUB(iid, mid);
    }

    public static void FSUB() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FSUB(-1, -1);
    }

    public static void FSUB(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FSUB(iid, mid);
    }

    public static void DSUB() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DSUB(-1, -1);
    }

    public static void DSUB(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DSUB(iid, mid);
    }

    public static void IMUL() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IMUL(-1, -1);
    }

    public static void IMUL(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IMUL(iid, mid);
    }

    public static void LMUL() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LMUL(-1, -1);
    }

    public static void LMUL(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LMUL(iid, mid);
    }

    public static void FMUL() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FMUL(-1, -1);
    }

    public static void FMUL(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FMUL(iid, mid);
    }

    public static void DMUL() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DMUL(-1, -1);
    }

    public static void DMUL(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DMUL(iid, mid);
    }

    public static void IDIV(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IDIV(iid, -1);
    }

    public static void IDIV(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IDIV(iid, mid);
    }

    public static void LDIV(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDIV(iid, -1);
    }

    public static void LDIV(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LDIV(iid, mid);
    }

    public static void FDIV() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FDIV(-1, -1);
    }

    public static void FDIV(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FDIV(iid, mid);
    }

    public static void DDIV() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DDIV(-1, -1);
    }

    public static void DDIV(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DDIV(iid, mid);
    }

    public static void IREM(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IREM(iid, -1);
    }

    public static void IREM(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IREM(iid, mid);
    }

    public static void LREM(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LREM(iid, -1);
    }

    public static void LREM(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LREM(iid, mid);
    }

    public static void FREM() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FREM(-1, -1);
    }

    public static void FREM(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FREM(iid, mid);
    }

    public static void DREM() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DREM(-1, -1);
    }

    public static void DREM(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DREM(iid, mid);
    }

    public static void INEG() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INEG(-1, -1);
    }

    public static void INEG(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INEG(iid, mid);
    }

    public static void LNEG() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LNEG(-1, -1);
    }

    public static void LNEG(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LNEG(iid, mid);
    }

    public static void FNEG() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FNEG(-1, -1);
    }

    public static void FNEG(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FNEG(iid, mid);
    }

    public static void DNEG() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DNEG(-1, -1);
    }

    public static void DNEG(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DNEG(iid, mid);
    }

    public static void ISHL() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ISHL(-1, -1);
    }

    public static void ISHL(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ISHL(iid, mid);
    }

    public static void LSHL() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LSHL(-1, -1);
    }

    public static void LSHL(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LSHL(iid, mid);
    }

    public static void ISHR() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ISHR(-1, -1);
    }

    public static void ISHR(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ISHR(iid, mid);
    }

    public static void LSHR() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LSHR(-1, -1);
    }

    public static void LSHR(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LSHR(iid, mid);
    }

    public static void IUSHR() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IUSHR(-1, -1);
    }

    public static void IUSHR(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IUSHR(iid, mid);
    }

    public static void LUSHR() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LUSHR(-1, -1);
    }

    public static void LUSHR(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LUSHR(iid, mid);
    }

    public static void IAND() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IAND(-1, -1);
    }

    public static void IAND(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IAND(iid, mid);
    }

    public static void LAND() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LAND(-1, -1);
    }

    public static void LAND(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LAND(iid, mid);
    }

    public static void IOR() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IOR(-1, -1);
    }

    public static void IOR(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IOR(iid, mid);
    }

    public static void LOR() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LOR(-1, -1);
    }

    public static void LOR(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LOR(iid, mid);
    }

    public static void IXOR() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IXOR(-1, -1);
    }

    public static void IXOR(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IXOR(iid, mid);
    }

    public static void LXOR() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LXOR(-1, -1);
    }

    public static void LXOR(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LXOR(iid, mid);
    }

    public static void I2L() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2L(-1, -1);
    }

    public static void I2L(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2L(iid, mid);
    }

    public static void I2F() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2F(-1, -1);
    }

    public static void I2F(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2F(iid, mid);
    }

    public static void I2D() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2D(-1, -1);
    }

    public static void I2D(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2D(iid, mid);
    }

    public static void L2I() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.L2I(-1, -1);
    }

    public static void L2I(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.L2I(iid, mid);
    }

    public static void L2F() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.L2F(-1, -1);
    }

    public static void L2F(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.L2F(iid, mid);
    }

    public static void L2D() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.L2D(-1, -1);
    }

    public static void L2D(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.L2D(iid, mid);
    }

    public static void F2I() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.F2I(-1, -1);
    }

    public static void F2I(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.F2I(iid, mid);
    }

    public static void F2L() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.F2L(-1, -1);
    }

    public static void F2L(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.F2L(iid, mid);
    }

    public static void F2D() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.F2D(-1, -1);
    }

    public static void F2D(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.F2D(iid, mid);
    }

    public static void D2I() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.D2I(-1, -1);
    }

    public static void D2I(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.D2I(iid, mid);
    }

    public static void D2L() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.D2L(-1, -1);
    }

    public static void D2L(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.D2L(iid, mid);
    }

    public static void D2F() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.D2F(-1, -1);
    }

    public static void D2F(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.D2F(iid, mid);
    }

    public static void I2B() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2B(-1, -1);
    }

    public static void I2B(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2B(iid, mid);
    }

    public static void I2C() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2C(-1, -1);
    }

    public static void I2C(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2C(iid, mid);
    }

    public static void I2S() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2S(-1, -1);
    }

    public static void I2S(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.I2S(iid, mid);
    }

    public static void LCMP() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LCMP(-1, -1);
    }

    public static void LCMP(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LCMP(iid, mid);
    }

    public static void FCMPL() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCMPL(-1, -1);
    }

    public static void FCMPL(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCMPL(iid, mid);
    }

    public static void FCMPG() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCMPG(-1, -1);
    }

    public static void FCMPG(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FCMPG(iid, mid);
    }

    public static void DCMPL() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DCMPL(-1, -1);
    }

    public static void DCMPL(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DCMPL(iid, mid);
    }

    public static void DCMPG() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DCMPG(-1, -1);
    }

    public static void DCMPG(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DCMPG(iid, mid);
    }

    public static void IRETURN(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IRETURN(iid, -1);
    }

    public static void IRETURN(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.IRETURN(iid, mid);
    }

    public static void LRETURN(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LRETURN(iid, -1);
    }

    public static void LRETURN(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LRETURN(iid, mid);
    }

    public static void FRETURN(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FRETURN(iid, -1);
    }

    public static void FRETURN(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.FRETURN(iid, mid);
    }

    public static void DRETURN(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DRETURN(iid, -1);
    }

    public static void DRETURN(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.DRETURN(iid, mid);
    }

    public static void ARETURN(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ARETURN(iid, -1);
    }

    public static void ARETURN(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ARETURN(iid, mid);
    }

    public static void RETURN(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.RETURN(iid, -1);
    }

    public static void RETURN(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.RETURN(iid, mid);
    }

    public static void ARRAYLENGTH(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ARRAYLENGTH(iid, -1);
    }

    public static void ARRAYLENGTH(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ARRAYLENGTH(iid, mid);
    }

    public static void ATHROW(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ATHROW(iid, -1);
    }

    public static void ATHROW(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.ATHROW(iid, mid);
    }

    public static void MONITORENTER(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.MONITORENTER(iid, -1);
    }

    public static void MONITORENTER(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.MONITORENTER(iid, mid);
    }

    public static void MONITOREXIT(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.MONITOREXIT(iid, -1);
    }

    public static void MONITOREXIT(int iid, int mid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.MONITOREXIT(iid, mid);
    }

    public static void GETVALUE_double(double v, int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_double(v, i);
    }

    public static void GETVALUE_long(long v, int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_long(v, i);
    }

    public static void GETVALUE_Object(Object v, int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_Object(v, i);
    }

    public static void GETVALUE_boolean(boolean v, int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_boolean(v, i);
    }

    public static void GETVALUE_byte(byte v, int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_byte(v, i);
    }

    public static void GETVALUE_char(char v, int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_char(v, i);
    }

    public static void GETVALUE_float(float v, int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_float(v, i);
    }

    public static void GETVALUE_int(int v, int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_int(v, i);
    }

    public static void GETVALUE_short(short v, int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_short(v, i);
    }

    public static void GETVALUE_void() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.GETVALUE_void();
    }

    public static void INVOKEMETHOD_EXCEPTION() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKEMETHOD_EXCEPTION();
    }

    public static void INVOKEMETHOD_END() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.INVOKEMETHOD_END();
    }

    public static void SPECIAL(int i) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.SPECIAL(i);
    }

    public static void MAKE_SYMBOLIC() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.MAKE_SYMBOLIC();
    }

    public static void LOOP_BEGIN(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LOOP_BEGIN(iid);
    }

    public static void LOOP_END(int iid) {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.LOOP_END(iid);
    }

    public static void flush() {
        Logger logger = ThreadHandler.getLogger(Thread.currentThread().getId());
        logger.flush();
    }
}
