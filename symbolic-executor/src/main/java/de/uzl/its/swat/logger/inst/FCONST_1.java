package de.uzl.its.swat.logger.inst;

/**
 * FCONST_1 - Push float (1.0f). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fconst_f">Java VM
 * specification</a>.
 */
public class FCONST_1 extends Instruction {

    /**
     * Creates a new FCONST_1 instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FCONST_1(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitFCONST_1(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FCONST_1");
    }
}
