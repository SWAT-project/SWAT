package de.uzl.its.swat.logger.inst;

/**
 * DREM - Remainder double.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.drem">Java VM specification</a>.
 */
public class DREM extends Instruction {

    /**
     * Creates a new DREM instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public DREM(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitDREM(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DREM");
    }
}
