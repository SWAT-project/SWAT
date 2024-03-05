package de.uzl.its.swat.logger.inst;

/**
 * AASTORE - Store into reference array For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.aastore">Java VM
 * specification</a>.
 */
public class AASTORE extends Instruction {
    /**
     * Creates a new AASTORE instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public AASTORE(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitAASTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("AASTORE");
    }
}
