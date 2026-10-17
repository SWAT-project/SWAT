package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

public interface IVisitor {
    void visitAALOAD(AALOAD inst) throws SymbolicInstructionException;

    void visitAASTORE(AASTORE inst) throws SymbolicInstructionException;

    void visitACONST_NULL(ACONST_NULL inst) throws SymbolicInstructionException;

    void visitALOAD(ALOAD inst) throws SymbolicInstructionException;

    void visitANEWARRAY(ANEWARRAY inst) throws SymbolicInstructionException;

    void visitARETURN(ARETURN inst) throws SymbolicInstructionException;

    void visitARRAYLENGTH(ARRAYLENGTH inst) throws SymbolicInstructionException;

    void visitASTORE(ASTORE inst) throws SymbolicInstructionException;

    void visitATHROW(ATHROW inst) throws SymbolicInstructionException;

    void visitBALOAD(BALOAD inst) throws SymbolicInstructionException;

    void visitBASTORE(BASTORE inst) throws SymbolicInstructionException;

    void visitBIPUSH(BIPUSH inst) throws SymbolicInstructionException;

    void visitCALOAD(CALOAD inst) throws SymbolicInstructionException;

    void visitCASTORE(CASTORE inst) throws SymbolicInstructionException;

    void visitCHECKCAST(CHECKCAST inst) throws SymbolicInstructionException;

    void visitCLINIT(CLINIT inst) throws SymbolicInstructionException;

    void visitD2F(D2F inst) throws SymbolicInstructionException;

    void visitD2I(D2I inst) throws SymbolicInstructionException;

    void visitD2L(D2L inst) throws SymbolicInstructionException;

    void visitDADD(DADD inst) throws SymbolicInstructionException;

    void visitDALOAD(DALOAD inst) throws SymbolicInstructionException;

    void visitDASTORE(DASTORE inst) throws SymbolicInstructionException;

    void visitDCMPG(DCMPG inst) throws SymbolicInstructionException;

    void visitDCMPL(DCMPL inst) throws SymbolicInstructionException;

    void visitDCONST_0(DCONST_0 inst) throws SymbolicInstructionException;

    void visitDCONST_1(DCONST_1 inst) throws SymbolicInstructionException;

    void visitDDIV(DDIV inst) throws SymbolicInstructionException;

    void visitDLOAD(DLOAD inst) throws SymbolicInstructionException;

    void visitDMUL(DMUL inst) throws SymbolicInstructionException;

    void visitDNEG(DNEG inst) throws SymbolicInstructionException;

    void visitDREM(DREM inst) throws SymbolicInstructionException;

    void visitDRETURN(DRETURN inst) throws SymbolicInstructionException;

    void visitDSTORE(DSTORE inst) throws SymbolicInstructionException;

    void visitDSUB(DSUB inst) throws SymbolicInstructionException;

    void visitDUP(DUP inst) throws SymbolicInstructionException;

    void visitDUP2(DUP2 inst) throws SymbolicInstructionException;

    void visitDUP2_X1(DUP2_X1 inst) throws SymbolicInstructionException;

    void visitDUP2_X2(DUP2_X2 inst) throws SymbolicInstructionException;

    void visitDUP_X1(DUP_X1 inst) throws SymbolicInstructionException;

    void visitDUP_X2(DUP_X2 inst) throws SymbolicInstructionException;

    void visitF2D(F2D inst) throws SymbolicInstructionException;

    void visitF2I(F2I inst) throws SymbolicInstructionException;

    void visitF2L(F2L inst) throws SymbolicInstructionException;

    void visitFADD(FADD inst) throws SymbolicInstructionException;

    void visitFALOAD(FALOAD inst) throws SymbolicInstructionException;

    void visitFASTORE(FASTORE inst) throws SymbolicInstructionException;

    void visitFCMPG(FCMPG inst) throws SymbolicInstructionException;

    void visitFCMPL(FCMPL inst) throws SymbolicInstructionException;

    void visitFCONST_0(FCONST_0 inst) throws SymbolicInstructionException;

    void visitFCONST_1(FCONST_1 inst) throws SymbolicInstructionException;

    void visitFCONST_2(FCONST_2 inst) throws SymbolicInstructionException;

    void visitFDIV(FDIV inst) throws SymbolicInstructionException;

    void visitFLOAD(FLOAD inst) throws SymbolicInstructionException;

    void visitFMUL(FMUL inst) throws SymbolicInstructionException;

    void visitFNEG(FNEG inst) throws SymbolicInstructionException;

    void visitFREM(FREM inst) throws SymbolicInstructionException;

    void visitFRETURN(FRETURN inst) throws SymbolicInstructionException;

    void visitFSTORE(FSTORE inst) throws SymbolicInstructionException;

    void visitFSUB(FSUB inst) throws SymbolicInstructionException;

    void visitGETFIELD(GETFIELD inst) throws SymbolicInstructionException;

    void visitGETSTATIC(GETSTATIC inst) throws SymbolicInstructionException;

    void visitGETVALUE_Object(GETVALUE_Object<?> inst) throws SymbolicInstructionException;

    void visitGETVALUE_boolean(GETVALUE_boolean inst) throws SymbolicInstructionException;

    void visitGETVALUE_byte(GETVALUE_byte inst) throws SymbolicInstructionException;

    void visitGETVALUE_char(GETVALUE_char inst) throws SymbolicInstructionException;

    void visitGETVALUE_double(GETVALUE_double inst) throws SymbolicInstructionException;

    void visitGETVALUE_float(GETVALUE_float inst) throws SymbolicInstructionException;

    void visitGETVALUE_int(GETVALUE_int inst) throws SymbolicInstructionException;

    void visitGETVALUE_long(GETVALUE_long inst) throws SymbolicInstructionException;

    void visitGETVALUE_short(GETVALUE_short inst) throws SymbolicInstructionException;

    void visitGETVALUE_void(GETVALUE_void inst) throws SymbolicInstructionException;

    void visitGOTO(GOTO inst) throws SymbolicInstructionException;

    void visitI2B(I2B inst) throws SymbolicInstructionException;

    void visitI2C(I2C inst) throws SymbolicInstructionException;

    void visitI2D(I2D inst) throws SymbolicInstructionException;

    void visitI2F(I2F inst) throws SymbolicInstructionException;

    void visitI2L(I2L inst) throws SymbolicInstructionException;

    void visitI2S(I2S inst) throws SymbolicInstructionException;

    void visitIADD(IADD inst) throws SymbolicInstructionException;

    void visitIALOAD(IALOAD inst) throws SymbolicInstructionException;

    void visitIAND(IAND inst) throws SymbolicInstructionException;

    void visitIASTORE(IASTORE inst) throws SymbolicInstructionException;

    void visitICONST_0(ICONST_0 inst) throws SymbolicInstructionException;

    void visitICONST_1(ICONST_1 inst) throws SymbolicInstructionException;

    void visitICONST_2(ICONST_2 inst) throws SymbolicInstructionException;

    void visitICONST_3(ICONST_3 inst) throws SymbolicInstructionException;

    void visitICONST_4(ICONST_4 inst) throws SymbolicInstructionException;

    void visitICONST_5(ICONST_5 inst) throws SymbolicInstructionException;

    void visitICONST_M1(ICONST_M1 inst) throws SymbolicInstructionException;

    void visitIDIV(IDIV inst) throws SymbolicInstructionException;

    void visitIFEQ(IFEQ inst) throws SymbolicInstructionException;

    void visitIFGE(IFGE inst) throws SymbolicInstructionException;

    void visitIFGT(IFGT inst) throws SymbolicInstructionException;

    void visitIFLE(IFLE inst) throws SymbolicInstructionException;

    void visitIFLT(IFLT inst) throws SymbolicInstructionException;

    void visitIFNE(IFNE inst) throws SymbolicInstructionException;

    void visitIFNONNULL(IFNONNULL inst) throws SymbolicInstructionException;

    void visitIFNULL(IFNULL inst) throws SymbolicInstructionException;

    void visitIF_ACMPEQ(IF_ACMPEQ inst) throws SymbolicInstructionException;

    void visitIF_ACMPNE(IF_ACMPNE inst) throws SymbolicInstructionException;

    void visitIF_ICMPEQ(IF_ICMPEQ inst) throws SymbolicInstructionException;

    void visitIF_ICMPGE(IF_ICMPGE inst) throws SymbolicInstructionException;

    void visitIF_ICMPGT(IF_ICMPGT inst) throws SymbolicInstructionException;

    void visitIF_ICMPLE(IF_ICMPLE inst) throws SymbolicInstructionException;

    void visitIF_ICMPLT(IF_ICMPLT inst) throws SymbolicInstructionException;

    void visitIF_ICMPNE(IF_ICMPNE inst) throws SymbolicInstructionException;

    void visitIINC(IINC inst) throws SymbolicInstructionException;

    void visitILOAD(ILOAD inst) throws SymbolicInstructionException;

    void visitIMUL(IMUL inst) throws SymbolicInstructionException;

    void visitINEG(INEG inst) throws SymbolicInstructionException;

    void visitINSTANCEOF(INSTANCEOF inst) throws SymbolicInstructionException;

    void visitINVOKEDYNAMIC(INVOKEDYNAMIC inst) throws SymbolicInstructionException;

    void visitINVOKEINTERFACE(INVOKEINTERFACE inst) throws SymbolicInstructionException;

    void visitINVOKEMETHOD_EXCEPTION(INVOKEMETHOD_EXCEPTION inst) throws SymbolicInstructionException;

    void visitINVOKESPECIAL(INVOKESPECIAL inst) throws SymbolicInstructionException;

    void visitINVOKESTATIC(INVOKESTATIC inst) throws SymbolicInstructionException;

    void visitINVOKEVIRTUAL(INVOKEVIRTUAL inst) throws SymbolicInstructionException;

    void visitIOR(IOR inst) throws SymbolicInstructionException;

    void visitIREM(IREM inst) throws SymbolicInstructionException;

    void visitIRETURN(IRETURN inst) throws SymbolicInstructionException;

    void visitISHL(ISHL inst) throws SymbolicInstructionException;

    void visitISHR(ISHR inst) throws SymbolicInstructionException;

    void visitISTORE(ISTORE inst) throws SymbolicInstructionException;

    void visitISUB(ISUB inst) throws SymbolicInstructionException;

    void visitIUSHR(IUSHR inst) throws SymbolicInstructionException;

    void visitIXOR(IXOR inst) throws SymbolicInstructionException;

    void visitJSR(JSR inst) throws SymbolicInstructionException;

    void visitL2D(L2D inst) throws SymbolicInstructionException;

    void visitL2F(L2F inst) throws SymbolicInstructionException;

    void visitL2I(L2I inst) throws SymbolicInstructionException;

    void visitLADD(LADD inst) throws SymbolicInstructionException;

    void visitLALOAD(LALOAD inst) throws SymbolicInstructionException;

    void visitLAND(LAND inst) throws SymbolicInstructionException;

    void visitLASTORE(LASTORE inst) throws SymbolicInstructionException;

    void visitLCMP(LCMP inst) throws SymbolicInstructionException;

    void visitLCONST_0(LCONST_0 inst) throws SymbolicInstructionException;

    void visitLCONST_1(LCONST_1 inst) throws SymbolicInstructionException;

    void visitLDC_String(LDC_String inst) throws SymbolicInstructionException;

    void visitLDC_double(LDC_double inst) throws SymbolicInstructionException;

    void visitLDC_float(LDC_float inst) throws SymbolicInstructionException;

    void visitLDC_int(LDC_int inst) throws SymbolicInstructionException;

    void visitLDC_long(LDC_long inst) throws SymbolicInstructionException;

    void visitLDC_Object(LDC_Object inst) throws SymbolicInstructionException;

    void visitLDIV(LDIV inst) throws SymbolicInstructionException;

    void visitLLOAD(LLOAD inst) throws SymbolicInstructionException;

    void visitLMUL(LMUL inst) throws SymbolicInstructionException;

    void visitLNEG(LNEG inst) throws SymbolicInstructionException;

    void visitLOOKUPSWITCH(LOOKUPSWITCH inst) throws SymbolicInstructionException;

    void visitLOR(LOR inst) throws SymbolicInstructionException;

    void visitLREM(LREM inst) throws SymbolicInstructionException;

    void visitLRETURN(LRETURN inst) throws SymbolicInstructionException;

    void visitLSHL(LSHL inst) throws SymbolicInstructionException;

    void visitLSHR(LSHR inst) throws SymbolicInstructionException;

    void visitLSTORE(LSTORE inst) throws SymbolicInstructionException;

    void visitLSUB(LSUB inst) throws SymbolicInstructionException;

    void visitLUSHR(LUSHR inst) throws SymbolicInstructionException;

    void visitLXOR(LXOR inst) throws SymbolicInstructionException;

    void visitMONITORENTER(MONITORENTER inst) throws SymbolicInstructionException;

    void visitMONITOREXIT(MONITOREXIT inst) throws SymbolicInstructionException;

    void visitMULTIANEWARRAY(MULTIANEWARRAY inst) throws SymbolicInstructionException;

    void visitNEW(NEW inst) throws SymbolicInstructionException;

    void visitNEWARRAY(NEWARRAY inst) throws SymbolicInstructionException;

    void visitNOP(NOP inst) throws SymbolicInstructionException;

    void visitPOP(POP inst) throws SymbolicInstructionException;

    void visitPOP2(POP2 inst) throws SymbolicInstructionException;

    void visitPUTFIELD(PUTFIELD inst) throws SymbolicInstructionException;

    void visitPUTSTATIC(PUTSTATIC inst) throws SymbolicInstructionException;

    void visitRET(RET inst) throws SymbolicInstructionException;

    void visitRETURN(RETURN inst) throws SymbolicInstructionException;

    void visitSALOAD(SALOAD inst) throws SymbolicInstructionException;

    void visitSASTORE(SASTORE inst) throws SymbolicInstructionException;

    void visitSIPUSH(SIPUSH inst) throws SymbolicInstructionException;

    void visitSWAP(SWAP inst) throws SymbolicInstructionException;

    void visitTABLESWITCH(TABLESWITCH inst) throws SymbolicInstructionException;

    void visitINVOKEMETHOD_END(INVOKEMETHOD_END inst) throws SymbolicInstructionException;

    void visitINVOKECLINIT_END(INVOKECLINIT_END inst) throws SymbolicInstructionException;

    void visitMAKE_SYMBOLIC(MAKE_SYMBOLIC inst) throws SymbolicInstructionException;

    void visitLOOP_BEGIN(LOOP_BEGIN inst) throws SymbolicInstructionException;

    void visitLOOP_END(LOOP_END inst) throws SymbolicInstructionException;

    void visitSPECIAL(SPECIAL inst) throws SymbolicInstructionException;

    void setNextInst(Instruction inst) throws SymbolicInstructionException;

    void visitUNPACK_INVOKE_PARAMETER(UNPACK_INVOKE_PARAMETER inst) throws SymbolicInstructionException;

    void visitSET_FIELD_REFLECTION(SET_FIELD_REFLECTION inst) throws SymbolicInstructionException;

    void visitGET_FIELD_REFLECTION(GET_FIELD_REFLECTION inst) throws SymbolicInstructionException;
}
