package de.uzl.its.swat.symbolic.instruction;

/**
 * IALOAD - Load int from array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.iaload">Java VM
 * specification</a>.
 */
public class IALOAD extends Instruction {

    /**
     * Creates a new IALOAD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public IALOAD(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitIALOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IALOAD");
    }
}
