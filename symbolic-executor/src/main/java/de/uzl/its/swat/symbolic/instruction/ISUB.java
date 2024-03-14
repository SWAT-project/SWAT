package de.uzl.its.swat.symbolic.instruction;

/**
 * ISUB - Subtract int. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.isub">Java VM
 * specification</a>.
 */
public class ISUB extends Instruction {

    /**
     * Creates a new ISUB instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public ISUB(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitISUB(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ISUB");
    }
}
