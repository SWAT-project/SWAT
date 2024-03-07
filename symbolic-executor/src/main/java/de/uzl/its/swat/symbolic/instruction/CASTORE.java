package de.uzl.its.swat.symbolic.instruction;
/**
 * CASTORE - Store into char array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.castore">Java VM
 * specification</a>.
 */
public class CASTORE extends Instruction {
    /**
     * Creates a new CASTORE instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public CASTORE(int iid, int mid) {
        super(iid, mid);
    }
    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitCASTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("CASTORE");
    }
}
