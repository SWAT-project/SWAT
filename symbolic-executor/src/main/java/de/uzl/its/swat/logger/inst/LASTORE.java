package de.uzl.its.swat.logger.inst;

/**
 * LASTORE - Store into long array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lastore">Java VM
 * specification</a>.
 */
public class LASTORE extends Instruction {

    /**
     * Creates a new LASTORE instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public LASTORE(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLASTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LASTORE");
    }
}
