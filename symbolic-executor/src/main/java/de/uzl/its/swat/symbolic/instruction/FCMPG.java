package de.uzl.its.swat.symbolic.instruction;

/**
 * FCMPG - Compare float (greater than). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fcmp_op">Java VM
 * specification</a>.
 */
public class FCMPG extends Instruction {

    /**
     * Creates a new FCMPG instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FCMPG(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitFCMPG(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FCMPG");
    }
}
