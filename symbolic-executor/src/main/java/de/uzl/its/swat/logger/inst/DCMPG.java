package de.uzl.its.swat.logger.inst;

/**
 * DCMPG - Compare double (greater than).
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dcmpg">Java VM specification</a>.
 */
public class DCMPG extends Instruction {

    /**
     * Creates a new DCMPG instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public DCMPG(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitDCMPG(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DCMPG");
    }
}
