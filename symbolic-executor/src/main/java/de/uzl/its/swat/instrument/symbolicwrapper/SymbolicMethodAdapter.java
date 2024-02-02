package de.uzl.its.swat.instrument.symbolicwrapper;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.AbstractMethodAdapter;
import de.uzl.its.swat.logger.SystemLogger;
import java.util.logging.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SymbolicMethodAdapter extends AbstractMethodAdapter {
    private boolean headerPossiblyInserted = false;

    private String cname;
    private final Config config = Config.instance();
    private final Logger logger;
    private final SystemLogger systemLogger;

    /**
     * Constructor that calls the super from the default MethodVisitor
     *
     * @param mv Parent MethodVisitor
     * @param name The method name
     * @param desc A string description of the parameters of the method
     */
    public SymbolicMethodAdapter(MethodVisitor mv, String cname, String name, String desc) {
        super(mv, name, desc);
        this.cname = cname;

        systemLogger = new SystemLogger();
        logger = systemLogger.getLogger();
    }

    @Override
    public void visitCode() {
        if (!headerPossiblyInserted) {
            insertHeaderIfNeeded();
            headerPossiblyInserted = true;
        }
    }

    /**
     * Visits a zero operand instruction. Adds the ending call for the concolic engine
     *
     * @param opcode the opcode of the instruction to be visited. This opcode is either NOP,
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
        switch (opcode) {
            case Opcodes.IRETURN,
                    Opcodes.FRETURN,
                    Opcodes.ARETURN,
                    Opcodes.LRETURN,
                    Opcodes.DRETURN,
                    Opcodes.RETURN -> addSolver();
            default -> {}
        }

        super.visitInsn(opcode);
    }

    /**
     * Starts the visit of the method's code, if any (i.e. non abstract method). Adds the beginning
     * call for the concolic engine to the method
     */
    public void insertHeaderIfNeeded() {
        addInitializer();
    }

    /** Adds the initializer call */
    private void addInitializer() {
        String endpointID = cname + "/" + this.getName() + this.getDesc();
        systemLogger.addToBox("    => Adding initializer: Main.init(endpointID)");
        visitLdcInsn(endpointID);
        visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "de/uzl/its/swat/Main",
                "init",
                "(Ljava/lang/String;)V",
                false);
    }

    /** Adds the ending call */
    private void addSolver() {
        // Add a call to solve and reset the symbolic engine

        systemLogger.addToBox("    => Adding termination: Main.terminate()");

        visitMethodInsn(Opcodes.INVOKESTATIC, "de/uzl/its/swat/Main", "terminate", "()V", false);
    }
}
