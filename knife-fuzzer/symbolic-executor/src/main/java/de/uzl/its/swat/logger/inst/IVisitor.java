package de.uzl.its.swat.logger.inst;

public interface IVisitor {
    void visitAALOAD(AALOAD inst);

    void visitAASTORE(AASTORE inst);

    void visitACONST_NULL(ACONST_NULL inst);

    void visitALOAD(ALOAD inst);

    void visitANEWARRAY(ANEWARRAY inst);

    void visitARETURN(ARETURN inst);

    void visitARRAYLENGTH(ARRAYLENGTH inst);

    void visitASTORE(ASTORE inst);

    void visitATHROW(ATHROW inst);

    void visitBALOAD(BALOAD inst);

    void visitBASTORE(BASTORE inst);

    void visitBIPUSH(BIPUSH inst);

    void visitCALOAD(CALOAD inst);

    void visitCASTORE(CASTORE inst);

    void visitCHECKCAST(CHECKCAST inst);

    void visitD2F(D2F inst);

    void visitD2I(D2I inst);

    void visitD2L(D2L inst);

    void visitDADD(DADD inst);

    void visitDALOAD(DALOAD inst);

    void visitDASTORE(DASTORE inst);

    void visitDCMPG(DCMPG inst);

    void visitDCMPL(DCMPL inst);

    void visitDCONST_0(DCONST_0 inst);

    void visitDCONST_1(DCONST_1 inst);

    void visitDDIV(DDIV inst);

    void visitDLOAD(DLOAD inst);

    void visitDMUL(DMUL inst);

    void visitDNEG(DNEG inst);

    void visitDREM(DREM inst);

    void visitDRETURN(DRETURN inst);

    void visitDSTORE(DSTORE inst);

    void visitDSUB(DSUB inst);

    void visitDUP(DUP inst);

    void visitDUP2(DUP2 inst);

    void visitDUP2_X1(DUP2_X1 inst);

    void visitDUP2_X2(DUP2_X2 inst);

    void visitDUP_X1(DUP_X1 inst);

    void visitDUP_X2(DUP_X2 inst);

    void visitF2D(F2D inst);

    void visitF2I(F2I inst);

    void visitF2L(F2L inst);

    void visitFADD(FADD inst);

    void visitFALOAD(FALOAD inst);

    void visitFASTORE(FASTORE inst);

    void visitFCMPG(FCMPG inst);

    void visitFCMPL(FCMPL inst);

    void visitFCONST_0(FCONST_0 inst);

    void visitFCONST_1(FCONST_1 inst);

    void visitFCONST_2(FCONST_2 inst);

    void visitFDIV(FDIV inst);

    void visitFLOAD(FLOAD inst);

    void visitFMUL(FMUL inst);

    void visitFNEG(FNEG inst);

    void visitFREM(FREM inst);

    void visitFRETURN(FRETURN inst);

    void visitFSTORE(FSTORE inst);

    void visitFSUB(FSUB inst);

    void visitGETFIELD(GETFIELD inst);

    void visitGETSTATIC(GETSTATIC inst);

    void visitGETVALUE_Object(GETVALUE_Object<?> inst);

    void visitGETVALUE_boolean(GETVALUE_boolean inst);

    void visitGETVALUE_byte(GETVALUE_byte inst);

    void visitGETVALUE_char(GETVALUE_char inst);

    void visitGETVALUE_double(GETVALUE_double inst);

    void visitGETVALUE_float(GETVALUE_float inst);

    void visitGETVALUE_int(GETVALUE_int inst);

    void visitGETVALUE_long(GETVALUE_long inst);

    void visitGETVALUE_short(GETVALUE_short inst);

    void visitGETVALUE_void(GETVALUE_void inst);

    void visitGOTO(GOTO inst);

    void visitI2B(I2B inst);

    void visitI2C(I2C inst);

    void visitI2D(I2D inst);

    void visitI2F(I2F inst);

    void visitI2L(I2L inst);

    void visitI2S(I2S inst);

    void visitIADD(IADD inst);

    void visitIALOAD(IALOAD inst);

    void visitIAND(IAND inst);

    void visitIASTORE(IASTORE inst);

    void visitICONST_0(ICONST_0 inst);

    void visitICONST_1(ICONST_1 inst);

    void visitICONST_2(ICONST_2 inst);

    void visitICONST_3(ICONST_3 inst);

    void visitICONST_4(ICONST_4 inst);

    void visitICONST_5(ICONST_5 inst);

    void visitICONST_M1(ICONST_M1 inst);

    void visitIDIV(IDIV inst);

    void visitIFEQ(IFEQ inst);

    void visitIFGE(IFGE inst);

    void visitIFGT(IFGT inst);

    void visitIFLE(IFLE inst);

    void visitIFLT(IFLT inst);

    void visitIFNE(IFNE inst);

    void visitIFNONNULL(IFNONNULL inst);

    void visitIFNULL(IFNULL inst);

    void visitIF_ACMPEQ(IF_ACMPEQ inst);

    void visitIF_ACMPNE(IF_ACMPNE inst);

    void visitIF_ICMPEQ(IF_ICMPEQ inst);

    void visitIF_ICMPGE(IF_ICMPGE inst);

    void visitIF_ICMPGT(IF_ICMPGT inst);

    void visitIF_ICMPLE(IF_ICMPLE inst);

    void visitIF_ICMPLT(IF_ICMPLT inst);

    void visitIF_ICMPNE(IF_ICMPNE inst);

    void visitIINC(IINC inst);

    void visitILOAD(ILOAD inst);

    void visitIMUL(IMUL inst);

    void visitINEG(INEG inst);

    void visitINSTANCEOF(INSTANCEOF inst);

    void visitINVOKEDYNAMIC(INVOKEDYNAMIC inst);

    void visitINVOKEINTERFACE(INVOKEINTERFACE inst);

    void visitINVOKEMETHOD_EXCEPTION(INVOKEMETHOD_EXCEPTION inst);

    void visitINVOKESPECIAL(INVOKESPECIAL inst);

    void visitINVOKESTATIC(INVOKESTATIC inst);

    void visitINVOKEVIRTUAL(INVOKEVIRTUAL inst);

    void visitIOR(IOR inst);

    void visitIREM(IREM inst);

    void visitIRETURN(IRETURN inst);

    void visitISHL(ISHL inst);

    void visitISHR(ISHR inst);

    void visitISTORE(ISTORE inst);

    void visitISUB(ISUB inst);

    void visitIUSHR(IUSHR inst);

    void visitIXOR(IXOR inst);

    void visitJSR(JSR inst);

    void visitL2D(L2D inst);

    void visitL2F(L2F inst);

    void visitL2I(L2I inst);

    void visitLADD(LADD inst);

    void visitLALOAD(LALOAD inst);

    void visitLAND(LAND inst);

    void visitLASTORE(LASTORE inst);

    void visitLCMP(LCMP inst);

    void visitLCONST_0(LCONST_0 inst);

    void visitLCONST_1(LCONST_1 inst);

    void visitLDC_String(LDC_String inst);

    void visitLDC_double(LDC_double inst);

    void visitLDC_float(LDC_float inst);

    void visitLDC_int(LDC_int inst);

    void visitLDC_long(LDC_long inst);

    void visitLDC_Object(LDC_Object inst);

    void visitLDIV(LDIV inst);

    void visitLLOAD(LLOAD inst);

    void visitLMUL(LMUL inst);

    void visitLNEG(LNEG inst);

    void visitLOOKUPSWITCH(LOOKUPSWITCH inst);

    void visitLOR(LOR inst);

    void visitLREM(LREM inst);

    void visitLRETURN(LRETURN inst);

    void visitLSHL(LSHL inst);

    void visitLSHR(LSHR inst);

    void visitLSTORE(LSTORE inst);

    void visitLSUB(LSUB inst);

    void visitLUSHR(LUSHR inst);

    void visitLXOR(LXOR inst);

    void visitMONITORENTER(MONITORENTER inst);

    void visitMONITOREXIT(MONITOREXIT inst);

    void visitMULTIANEWARRAY(MULTIANEWARRAY inst);

    void visitNEW(NEW inst);

    void visitNEWARRAY(NEWARRAY inst);

    void visitNOP(NOP inst);

    void visitPOP(POP inst);

    void visitPOP2(POP2 inst);

    void visitPUTFIELD(PUTFIELD inst);

    void visitPUTSTATIC(PUTSTATIC inst);

    void visitRET(RET inst);

    void visitRETURN(RETURN inst);

    void visitSALOAD(SALOAD inst);

    void visitSASTORE(SASTORE inst);

    void visitSIPUSH(SIPUSH inst);

    void visitSWAP(SWAP inst);

    void visitTABLESWITCH(TABLESWITCH inst);

    void visitINVOKEMETHOD_END(INVOKEMETHOD_END inst);

    void visitMAKE_SYMBOLIC(MAKE_SYMBOLIC inst);

    void visitLOOP_BEGIN(LOOP_BEGIN inst);

    void visitLOOP_END(LOOP_END inst);

    void visitSPECIAL(SPECIAL inst);

    void setNext(Instruction next);
}
