package de.uzl.its.swat.logger.inst;

/**
 * SASTORE - Store into short array.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.sastore">Java VM specification</a>.
 */
public class SASTORE extends Instruction {

    /**
     * Creates a new SASTORE instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public SASTORE(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitSASTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("SASTORE");
    }
}