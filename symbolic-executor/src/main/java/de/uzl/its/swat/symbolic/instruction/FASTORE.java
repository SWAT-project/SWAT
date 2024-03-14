package de.uzl.its.swat.symbolic.instruction;

/**
 * FASTORE - Store into float array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fastore">Java VM
 * specification</a>.
 */
public class FASTORE extends Instruction {

    /**
     * Creates a new FASTORE instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FASTORE(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitFASTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FASTORE");
    }
}
