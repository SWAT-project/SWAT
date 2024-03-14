package de.uzl.its.swat.symbolic.instruction;

/**
 * DCONST_1 - Push double (1.0d). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dconst_d">Java VM
 * specification</a>.
 */
public class DCONST_1 extends Instruction {

    /**
     * Creates a new DCONST_1 instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public DCONST_1(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitDCONST_1(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DCONST_1");
    }
}
