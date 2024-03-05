package de.uzl.its.swat.logger.inst;
/**
 * BASTORE - Store into byte or boolean array.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.bastore">Java VM specification</a>.
 */
public class BASTORE extends Instruction {
    /**
     * Creates a new BASTORE instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public BASTORE(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitBASTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("BASTORE");
    }
}

