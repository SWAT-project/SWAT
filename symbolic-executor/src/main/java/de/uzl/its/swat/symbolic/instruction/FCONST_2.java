package de.uzl.its.swat.symbolic.instruction;

/**
 * FCONST_2 - Push float (2.0f). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fconst_f">Java VM
 * specification</a>.
 */
public class FCONST_2 extends Instruction {

    /**
     * Creates a new FCONST_2 instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FCONST_2(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitFCONST_2(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FCONST_2");
    }
}
