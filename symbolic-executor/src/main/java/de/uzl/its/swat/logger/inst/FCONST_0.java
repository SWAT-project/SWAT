package de.uzl.its.swat.logger.inst;

/**
 * FCONST_0 - Push float (0.0f). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fconst_f">Java VM
 * specification</a>.
 */
public class FCONST_0 extends Instruction {

    /**
     * Creates a new FCONST_0 instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FCONST_0(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitFCONST_0(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FCONST_0");
    }
}
