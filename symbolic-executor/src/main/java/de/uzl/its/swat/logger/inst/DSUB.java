package de.uzl.its.swat.logger.inst;

/**
 * DSUB - Subtract double.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dsub">Java VM specification</a>.
 */
public class DSUB extends Instruction {

    /**
     * Creates a new DSUB instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public DSUB(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitDSUB(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DSUB");
    }
}
