package de.uzl.its.swat.logger.inst;

/**
 * LNEG - Negate long.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lneg">Java VM specification</a>.
 */
public class LNEG extends Instruction {

    /**
     * Creates a new LNEG instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public LNEG(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLNEG(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LNEG");
    }
}