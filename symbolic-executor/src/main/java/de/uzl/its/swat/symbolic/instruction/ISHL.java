package de.uzl.its.swat.symbolic.instruction;

/**
 * ISHL - Shift left int. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.ishl">Java VM
 * specification</a>.
 */
public class ISHL extends Instruction {

    /**
     * Creates a new ISHL instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public ISHL(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitISHL(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ISHL");
    }
}
