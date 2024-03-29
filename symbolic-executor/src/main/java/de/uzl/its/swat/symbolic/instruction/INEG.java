package de.uzl.its.swat.symbolic.instruction;

/**
 * INEG - Negate int. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.ineg">Java VM
 * specification</a>.
 */
public class INEG extends Instruction {

    /**
     * Creates a new INEG instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public INEG(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitINEG(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("INEG");
    }
}
