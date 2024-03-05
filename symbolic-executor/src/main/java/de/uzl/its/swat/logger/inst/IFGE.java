package de.uzl.its.swat.logger.inst;

/**
 * IFGE - Branch if int comparison with zero succeeds (greater or equal)
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.if_cond">Java VM specification</a>.
 */
public class IFGE extends Instruction {
    // The jump destination
    int label;


    /**
     * Creates a new IFGE instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param label the jump destination
     */
    public IFGE(int iid, int mid, int label) {
        super(iid, mid);
        this.label = label;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitIFGE(this);
    }


    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IFGE " + label);
    }
}
