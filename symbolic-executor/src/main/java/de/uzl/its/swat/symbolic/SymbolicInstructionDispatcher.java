package de.uzl.its.swat.symbolic;

import de.uzl.its.swat.symbolic.processor.InstructionProcessor;
import de.uzl.its.swat.thread.ThreadHandler;

public final class SymbolicInstructionDispatcher {

    private SymbolicInstructionDispatcher() {}

    public static void LDC(int iid, int c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, int c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    public static void LDC(int iid, long c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, long c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    public static void LDC(int iid, float c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, float c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    public static void LDC(int iid, double c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, double c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    public static void LDC(int iid, String c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, String c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    public static void LDC(int iid, Object c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, -1, c);
    }

    public static void LDC(int iid, int mid, Object c) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDC(iid, mid, c);
    }

    public static void IINC(int var, int increment) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IINC(-1, -1, var, increment);
    }

    public static void IINC(int iid, int mid, int var, int increment) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IINC(iid, mid, var, increment);
    }

    public static void MULTIANEWARRAY(int iid, String desc, int dims) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MULTIANEWARRAY(iid, -1, desc, dims);
    }

    public static void MULTIANEWARRAY(int iid, int mid, String desc, int dims) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MULTIANEWARRAY(iid, mid, desc, dims);
    }

    public static void LOOKUPSWITCH(int iid, int dflt, int[] keys, int[] labels) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOOKUPSWITCH(iid, -1, dflt, keys, labels);
    }

    public static void LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOOKUPSWITCH(iid, mid, dflt, keys, labels);
    }

    public static void TABLESWITCH(
            int iid, int min, int max, int dflt, int[] labels, int[] values) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.TABLESWITCH(iid, -1, min, max, dflt, labels, values);
    }

    public static void TABLESWITCH(
            int iid, int mid, int min, int max, int dflt, int[] labels, int[] values) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.TABLESWITCH(iid, -1, min, max, dflt, labels, values);
    }

    public static void IFEQ(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFEQ(iid, -1, label);
    }

    public static void IFEQ(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFEQ(iid, mid, label);
    }

    public static void IFNE(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNE(iid, -1, label);
    }

    public static void IFNE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNE(iid, mid, label);
    }

    public static void IFLT(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLT(iid, -1, label);
    }

    public static void IFLT(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLT(iid, mid, label);
    }

    public static void IFGE(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGE(iid, -1, label);
    }

    public static void IFGE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGE(iid, mid, label);
    }

    public static void IFGT(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGT(iid, -1, label);
    }

    public static void IFGT(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFGT(iid, mid, label);
    }

    public static void IFLE(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLE(iid, -1, label);
    }

    public static void IFLE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFLE(iid, mid, label);
    }

    public static void IF_ICMPEQ(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPEQ(iid, -1, label);
    }

    public static void IF_ICMPEQ(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPEQ(iid, mid, label);
    }

    public static void IF_ICMPNE(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPNE(iid, -1, label);
    }

    public static void IF_ICMPNE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPNE(iid, mid, label);
    }

    public static void IF_ICMPLT(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLT(iid, -1, label);
    }

    public static void IF_ICMPLT(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLT(iid, mid, label);
    }

    public static void IF_ICMPGE(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGE(iid, -1, label);
    }

    public static void IF_ICMPGE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGE(iid, mid, label);
    }

    public static void IF_ICMPGT(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGT(iid, -1, label);
    }

    public static void IF_ICMPGT(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPGT(iid, mid, label);
    }

    public static void IF_ICMPLE(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLE(iid, -1, label);
    }

    public static void IF_ICMPLE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ICMPLE(iid, mid, label);
    }

    public static void IF_ACMPEQ(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPEQ(iid, -1, label);
    }

    public static void IF_ACMPEQ(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPEQ(iid, mid, label);
    }

    public static void IF_ACMPNE(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPNE(iid, -1, label);
    }

    public static void IF_ACMPNE(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IF_ACMPNE(iid, mid, label);
    }

    public static void GOTO(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GOTO(iid, -1, label);
    }

    public static void GOTO(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GOTO(iid, mid, label);
    }

    public static void JSR(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.JSR(iid, -1, label);
    }

    public static void JSR(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.JSR(iid, mid, label);
    }

    public static void IFNULL(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNULL(iid, -1, label);
    }

    public static void IFNULL(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNULL(iid, mid, label);
    }

    public static void IFNONNULL(int iid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNONNULL(iid, -1, label);
    }

    public static void IFNONNULL(int iid, int mid, int label) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IFNONNULL(iid, mid, label);
    }

    public static void INVOKEVIRTUAL(int iid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEVIRTUAL(iid, -1, owner, name, desc);
    }

    public static void INVOKEVIRTUAL(int iid, int mid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEVIRTUAL(iid, mid, owner, name, desc);
    }

    public static void INVOKESPECIAL(int iid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESPECIAL(iid, -1, owner, name, desc);
    }

    public static void INVOKESPECIAL(int iid, int mid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESPECIAL(iid, mid, owner, name, desc);
    }

    public static void INVOKEDYNAMIC(
            int iid, String owner, String name, String desc, String lambda) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEDYNAMIC(iid, -1, owner, name, desc, lambda);
    }

    public static void INVOKEDYNAMIC(
            int iid, int mid, String owner, String name, String desc, String lambda) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEDYNAMIC(iid, mid, owner, name, desc, lambda);
    }

    public static void INVOKESTATIC(int iid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESTATIC(iid, -1, owner, name, desc);
    }

    public static void INVOKESTATIC(int iid, int mid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKESTATIC(iid, mid, owner, name, desc);
    }

    public static void INVOKEINTERFACE(int iid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEINTERFACE(iid, -1, owner, name, desc);
    }

    public static void INVOKEINTERFACE(int iid, int mid, String owner, String name, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEINTERFACE(iid, mid, owner, name, desc);
    }

    public static void GETSTATIC(int iid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETSTATIC(iid, -1, cIdx, fIdx, desc);
    }

    public static void GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETSTATIC(iid, mid, cIdx, fIdx, desc);
    }

    public static void PUTSTATIC(int iid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTSTATIC(iid, -1, cIdx, fIdx, desc);
    }

    public static void PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTSTATIC(iid, mid, cIdx, fIdx, desc);
    }

    public static void GETFIELD(int iid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETFIELD(iid, -1, cIdx, fIdx, desc);
    }

    public static void GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETFIELD(iid, mid, cIdx, fIdx, desc);
    }

    public static void PUTFIELD(int iid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTFIELD(iid, -1, cIdx, fIdx, desc);
    }

    public static void PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.PUTFIELD(iid, mid, cIdx, fIdx, desc);
    }

    public static void NEW(int iid, String type, int cIdx) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEW(iid, -1, type, cIdx);
    }

    public static void NEW(int iid, int mid, String type, int cIdx) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEW(iid, mid, type, cIdx);
    }

    public static void ANEWARRAY(int iid, String type) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ANEWARRAY(iid, -1, type);
    }

    public static void ANEWARRAY(int iid, int mid, String type) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ANEWARRAY(iid, mid, type);
    }

    public static void CHECKCAST(int iid, String type) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CHECKCAST(iid, -1, type);
    }

    public static void CHECKCAST(int iid, int mid, String type) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CHECKCAST(iid, mid, type);
    }

    public static void INSTANCEOF(int iid, String type) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INSTANCEOF(iid, -1, type);
    }

    public static void INSTANCEOF(int iid, int mid, String type) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INSTANCEOF(iid, mid, type);
    }

    public static void BIPUSH(int value) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BIPUSH(-1, -1, value);
    }

    public static void BIPUSH(int iid, int mid, int value) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BIPUSH(iid, mid, value);
    }

    public static void SIPUSH(int value) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SIPUSH(-1, -1, value);
    }

    public static void SIPUSH(int iid, int mid, int value) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SIPUSH(iid, mid, value);
    }

    public static void NEWARRAY(int iid, int atype) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEWARRAY(iid, -1, atype);
    }

    public static void NEWARRAY(int iid, int mid, int atype) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NEWARRAY(iid, mid, atype);
    }

    public static void ILOAD(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ILOAD(-1, -1, var);
    }

    public static void ILOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ILOAD(iid, mid, var);
    }

    public static void LLOAD(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LLOAD(-1, -1, var);
    }

    public static void LLOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LLOAD(iid, mid, var);
    }

    public static void FLOAD(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FLOAD(-1, -1, var);
    }

    public static void FLOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FLOAD(iid, mid, var);
    }

    public static void DLOAD(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DLOAD(-1, -1, var);
    }

    public static void DLOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DLOAD(iid, mid, var);
    }

    public static void ALOAD(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ALOAD(-1, -1, var);
    }

    public static void ALOAD(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ALOAD(iid, mid, var);
    }

    public static void ISTORE(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISTORE(-1, -1, var);
    }

    public static void ISTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISTORE(iid, mid, var);
    }

    public static void LSTORE(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSTORE(-1, -1, var);
    }

    public static void LSTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSTORE(iid, mid, var);
    }

    public static void FSTORE(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSTORE(-1, -1, var);
    }

    public static void FSTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSTORE(iid, mid, var);
    }

    public static void DSTORE(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSTORE(-1, -1, var);
    }

    public static void DSTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSTORE(iid, mid, var);
    }

    public static void ASTORE(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ASTORE(-1, -1, var);
    }

    public static void ASTORE(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ASTORE(iid, mid, var);
    }

    public static void RET(int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RET(-1, -1, var);
    }

    public static void RET(int iid, int mid, int var) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RET(iid, mid, var);
    }

    public static void NOP() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NOP(-1, -1);
    }

    public static void NOP(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.NOP(iid, mid);
    }

    public static void ACONST_NULL() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ACONST_NULL(-1, -1);
    }

    public static void ACONST_NULL(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ACONST_NULL(iid, mid);
    }

    public static void ICONST_M1() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_M1(-1, -1);
    }

    public static void ICONST_M1(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_M1(iid, mid);
    }

    public static void ICONST_0() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_0(-1, -1);
    }

    public static void ICONST_0(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_0(iid, mid);
    }

    public static void ICONST_1() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_1(-1, -1);
    }

    public static void ICONST_1(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_1(iid, mid);
    }

    public static void ICONST_2() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_2(-1, -1);
    }

    public static void ICONST_2(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_2(iid, mid);
    }

    public static void ICONST_3() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_3(-1, -1);
    }

    public static void ICONST_3(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_3(iid, mid);
    }

    public static void ICONST_4() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_4(-1, -1);
    }

    public static void ICONST_4(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_4(iid, mid);
    }

    public static void ICONST_5() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_5(-1, -1);
    }

    public static void ICONST_5(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ICONST_5(iid, mid);
    }

    public static void LCONST_0() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_0(-1, -1);
    }

    public static void LCONST_0(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_0(iid, mid);
    }

    public static void LCONST_1() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_1(-1, -1);
    }

    public static void LCONST_1(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCONST_1(iid, mid);
    }

    public static void FCONST_0() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_0(-1, -1);
    }

    public static void FCONST_0(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_0(iid, mid);
    }

    public static void FCONST_1() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_1(-1, -1);
    }

    public static void FCONST_1(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_1(iid, mid);
    }

    public static void FCONST_2() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_2(-1, -1);
    }

    public static void FCONST_2(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCONST_2(iid, mid);
    }

    public static void DCONST_0() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_0(-1, -1);
    }

    public static void DCONST_0(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_0(iid, mid);
    }

    public static void DCONST_1() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_1(-1, -1);
    }

    public static void DCONST_1(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCONST_1(iid, mid);
    }

    public static void IALOAD(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IALOAD(iid, -1);
    }

    public static void IALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IALOAD(iid, mid);
    }

    public static void LALOAD(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LALOAD(iid, -1);
    }

    public static void LALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LALOAD(iid, mid);
    }

    public static void FALOAD(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FALOAD(iid, -1);
    }

    public static void FALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FALOAD(iid, mid);
    }

    public static void DALOAD(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DALOAD(iid, -1);
    }

    public static void DALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DALOAD(iid, mid);
    }

    public static void AALOAD(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AALOAD(iid, -1);
    }

    public static void AALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AALOAD(iid, mid);
    }

    public static void BALOAD(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BALOAD(iid, -1);
    }

    public static void BALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BALOAD(iid, mid);
    }

    public static void CALOAD(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CALOAD(iid, -1);
    }

    public static void CALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CALOAD(iid, mid);
    }

    public static void SALOAD(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SALOAD(iid, -1);
    }

    public static void SALOAD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SALOAD(iid, mid);
    }

    public static void IASTORE(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IASTORE(iid, -1);
    }

    public static void IASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IASTORE(iid, mid);
    }

    public static void LASTORE(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LASTORE(iid, -1);
    }

    public static void LASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LASTORE(iid, mid);
    }

    public static void FASTORE(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FASTORE(iid, -1);
    }

    public static void FASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FASTORE(iid, mid);
    }

    public static void DASTORE(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DASTORE(iid, -1);
    }

    public static void DASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DASTORE(iid, mid);
    }

    public static void AASTORE(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AASTORE(iid, -1);
    }

    public static void AASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.AASTORE(iid, mid);
    }

    public static void BASTORE(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BASTORE(iid, -1);
    }

    public static void BASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.BASTORE(iid, mid);
    }

    public static void CASTORE(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CASTORE(iid, -1);
    }

    public static void CASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.CASTORE(iid, mid);
    }

    public static void SASTORE(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SASTORE(iid, -1);
    }

    public static void SASTORE(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SASTORE(iid, mid);
    }

    public static void POP() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP(-1, -1);
    }

    public static void POP(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP(iid, mid);
    }

    public static void POP2() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP2(-1, -1);
    }

    public static void POP2(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.POP2(iid, mid);
    }

    public static void DUP() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP(-1, -1);
    }

    public static void DUP(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP(iid, mid);
    }

    public static void DUP_X1() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X1(-1, -1);
    }

    public static void DUP_X1(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X1(iid, mid);
    }

    public static void DUP_X2() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X2(-1, -1);
    }

    public static void DUP_X2(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP_X2(iid, mid);
    }

    public static void DUP2() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2(-1, -1);
    }

    public static void DUP2(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2(iid, mid);
    }

    public static void DUP2_X1() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X1(-1, -1);
    }

    public static void DUP2_X1(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X1(iid, mid);
    }

    public static void DUP2_X2() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X2(-1, -1);
    }

    public static void DUP2_X2(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DUP2_X2(iid, mid);
    }

    public static void SWAP() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SWAP(-1, -1);
    }

    public static void SWAP(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SWAP(iid, mid);
    }

    public static void IADD() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IADD(-1, -1);
    }

    public static void IADD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IADD(iid, mid);
    }

    public static void LADD() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LADD(-1, -1);
    }

    public static void LADD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LADD(iid, mid);
    }

    public static void FADD() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FADD(-1, -1);
    }

    public static void FADD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FADD(iid, mid);
    }

    public static void DADD() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DADD(-1, -1);
    }

    public static void DADD(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DADD(iid, mid);
    }

    public static void ISUB() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISUB(-1, -1);
    }

    public static void ISUB(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISUB(iid, mid);
    }

    public static void LSUB() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSUB(-1, -1);
    }

    public static void LSUB(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSUB(iid, mid);
    }

    public static void FSUB() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSUB(-1, -1);
    }

    public static void FSUB(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FSUB(iid, mid);
    }

    public static void DSUB() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSUB(-1, -1);
    }

    public static void DSUB(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DSUB(iid, mid);
    }

    public static void IMUL() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IMUL(-1, -1);
    }

    public static void IMUL(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IMUL(iid, mid);
    }

    public static void LMUL() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LMUL(-1, -1);
    }

    public static void LMUL(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LMUL(iid, mid);
    }

    public static void FMUL() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FMUL(-1, -1);
    }

    public static void FMUL(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FMUL(iid, mid);
    }

    public static void DMUL() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DMUL(-1, -1);
    }

    public static void DMUL(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DMUL(iid, mid);
    }

    public static void IDIV(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IDIV(iid, -1);
    }

    public static void IDIV(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IDIV(iid, mid);
    }

    public static void LDIV(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDIV(iid, -1);
    }

    public static void LDIV(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LDIV(iid, mid);
    }

    public static void FDIV() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FDIV(-1, -1);
    }

    public static void FDIV(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FDIV(iid, mid);
    }

    public static void DDIV() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DDIV(-1, -1);
    }

    public static void DDIV(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DDIV(iid, mid);
    }

    public static void IREM(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IREM(iid, -1);
    }

    public static void IREM(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IREM(iid, mid);
    }

    public static void LREM(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LREM(iid, -1);
    }

    public static void LREM(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LREM(iid, mid);
    }

    public static void FREM() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FREM(-1, -1);
    }

    public static void FREM(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FREM(iid, mid);
    }

    public static void DREM() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DREM(-1, -1);
    }

    public static void DREM(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DREM(iid, mid);
    }

    public static void INEG() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INEG(-1, -1);
    }

    public static void INEG(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INEG(iid, mid);
    }

    public static void LNEG() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LNEG(-1, -1);
    }

    public static void LNEG(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LNEG(iid, mid);
    }

    public static void FNEG() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FNEG(-1, -1);
    }

    public static void FNEG(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FNEG(iid, mid);
    }

    public static void DNEG() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DNEG(-1, -1);
    }

    public static void DNEG(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DNEG(iid, mid);
    }

    public static void ISHL() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHL(-1, -1);
    }

    public static void ISHL(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHL(iid, mid);
    }

    public static void LSHL() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHL(-1, -1);
    }

    public static void LSHL(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHL(iid, mid);
    }

    public static void ISHR() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHR(-1, -1);
    }

    public static void ISHR(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ISHR(iid, mid);
    }

    public static void LSHR() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHR(-1, -1);
    }

    public static void LSHR(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LSHR(iid, mid);
    }

    public static void IUSHR() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IUSHR(-1, -1);
    }

    public static void IUSHR(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IUSHR(iid, mid);
    }

    public static void LUSHR() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LUSHR(-1, -1);
    }

    public static void LUSHR(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LUSHR(iid, mid);
    }

    public static void IAND() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IAND(-1, -1);
    }

    public static void IAND(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IAND(iid, mid);
    }

    public static void LAND() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LAND(-1, -1);
    }

    public static void LAND(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LAND(iid, mid);
    }

    public static void IOR() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IOR(-1, -1);
    }

    public static void IOR(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IOR(iid, mid);
    }

    public static void LOR() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOR(-1, -1);
    }

    public static void LOR(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOR(iid, mid);
    }

    public static void IXOR() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IXOR(-1, -1);
    }

    public static void IXOR(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IXOR(iid, mid);
    }

    public static void LXOR() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LXOR(-1, -1);
    }

    public static void LXOR(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LXOR(iid, mid);
    }

    public static void I2L() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2L(-1, -1);
    }

    public static void I2L(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2L(iid, mid);
    }

    public static void I2F() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2F(-1, -1);
    }

    public static void I2F(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2F(iid, mid);
    }

    public static void I2D() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2D(-1, -1);
    }

    public static void I2D(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2D(iid, mid);
    }

    public static void L2I() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2I(-1, -1);
    }

    public static void L2I(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2I(iid, mid);
    }

    public static void L2F() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2F(-1, -1);
    }

    public static void L2F(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2F(iid, mid);
    }

    public static void L2D() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2D(-1, -1);
    }

    public static void L2D(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.L2D(iid, mid);
    }

    public static void F2I() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2I(-1, -1);
    }

    public static void F2I(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2I(iid, mid);
    }

    public static void F2L() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2L(-1, -1);
    }

    public static void F2L(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2L(iid, mid);
    }

    public static void F2D() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2D(-1, -1);
    }

    public static void F2D(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.F2D(iid, mid);
    }

    public static void D2I() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2I(-1, -1);
    }

    public static void D2I(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2I(iid, mid);
    }

    public static void D2L() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2L(-1, -1);
    }

    public static void D2L(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2L(iid, mid);
    }

    public static void D2F() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2F(-1, -1);
    }

    public static void D2F(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.D2F(iid, mid);
    }

    public static void I2B() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2B(-1, -1);
    }

    public static void I2B(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2B(iid, mid);
    }

    public static void I2C() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2C(-1, -1);
    }

    public static void I2C(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2C(iid, mid);
    }

    public static void I2S() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2S(-1, -1);
    }

    public static void I2S(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.I2S(iid, mid);
    }

    public static void LCMP() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCMP(-1, -1);
    }

    public static void LCMP(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LCMP(iid, mid);
    }

    public static void FCMPL() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPL(-1, -1);
    }

    public static void FCMPL(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPL(iid, mid);
    }

    public static void FCMPG() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPG(-1, -1);
    }

    public static void FCMPG(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FCMPG(iid, mid);
    }

    public static void DCMPL() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPL(-1, -1);
    }

    public static void DCMPL(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPL(iid, mid);
    }

    public static void DCMPG() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPG(-1, -1);
    }

    public static void DCMPG(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DCMPG(iid, mid);
    }

    public static void IRETURN(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IRETURN(iid, -1);
    }

    public static void IRETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.IRETURN(iid, mid);
    }

    public static void LRETURN(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LRETURN(iid, -1);
    }

    public static void LRETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LRETURN(iid, mid);
    }

    public static void FRETURN(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FRETURN(iid, -1);
    }

    public static void FRETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.FRETURN(iid, mid);
    }

    public static void DRETURN(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DRETURN(iid, -1);
    }

    public static void DRETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.DRETURN(iid, mid);
    }

    public static void ARETURN(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARETURN(iid, -1);
    }

    public static void ARETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARETURN(iid, mid);
    }

    public static void RETURN(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RETURN(iid, -1);
    }

    public static void RETURN(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.RETURN(iid, mid);
    }

    public static void ARRAYLENGTH(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARRAYLENGTH(iid, -1);
    }

    public static void ARRAYLENGTH(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ARRAYLENGTH(iid, mid);
    }

    public static void ATHROW(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ATHROW(iid, -1);
    }

    public static void ATHROW(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.ATHROW(iid, mid);
    }

    public static void MONITORENTER(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITORENTER(iid, -1);
    }

    public static void MONITORENTER(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITORENTER(iid, mid);
    }

    public static void MONITOREXIT(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITOREXIT(iid, -1);
    }

    public static void MONITOREXIT(int iid, int mid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MONITOREXIT(iid, mid);
    }

    public static void GETVALUE_double(double v, int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_double(v, i);
    }

    public static void GETVALUE_long(long v, int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_long(v, i);
    }

    public static void GETVALUE_Object(Object v, int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_Object(v, i);
    }

    public static void GETVALUE_boolean(boolean v, int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_boolean(v, i);
    }

    public static void GETVALUE_byte(byte v, int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_byte(v, i);
    }

    public static void GETVALUE_char(char v, int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_char(v, i);
    }

    public static void GETVALUE_float(float v, int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_float(v, i);
    }

    public static void GETVALUE_int(int v, int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_int(v, i);
    }

    public static void GETVALUE_short(short v, int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_short(v, i);
    }

    public static void GETVALUE_void() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.GETVALUE_void();
    }

    public static void INVOKEMETHOD_EXCEPTION() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEMETHOD_EXCEPTION();
    }

    public static void INVOKEMETHOD_END() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.INVOKEMETHOD_END();
    }

    public static void SPECIAL(int i) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.SPECIAL(i);
    }

    public static void MAKE_SYMBOLIC() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.MAKE_SYMBOLIC();
    }

    public static void LOOP_BEGIN(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOOP_BEGIN(iid);
    }

    public static void LOOP_END(int iid) {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.LOOP_END(iid);
    }

    public static void flush() {
        InstructionProcessor instructionProcessor = ThreadHandler.getProcessor(Thread.currentThread().getId());
        instructionProcessor.flush();
    }
}
