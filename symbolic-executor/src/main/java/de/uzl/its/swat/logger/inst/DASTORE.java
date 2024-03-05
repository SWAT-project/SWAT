package de.uzl.its.swat.logger.inst;

/**
 * DASTORE - Store into double array.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dastore">Java VM specification</a>.
 */
public class DASTORE extends Instruction {

    /**
     * Creates a new DASTORE instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public DASTORE(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitDASTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DASTORE");
    }
}
