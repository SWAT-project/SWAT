package de.uzl.its.swat.logger.inst;

/**
 * IREM - Remainder int.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.irem">Java VM specification</a>.
 */
public class IREM extends Instruction {

    /**
     * Creates a new IREM instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public IREM(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitIREM(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IREM");
    }
}
