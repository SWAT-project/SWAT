package de.uzl.its.swat.symbolic.instruction;

/**
 * IASTORE - Store into int array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.iastore">Java VM
 * specification</a>.
 */
public class IASTORE extends Instruction {

    /**
     * Creates a new IASTORE instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public IASTORE(int iid, int mid) {
        super(iid, mid);
    }
    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitIASTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IASTORE");
    }
}
