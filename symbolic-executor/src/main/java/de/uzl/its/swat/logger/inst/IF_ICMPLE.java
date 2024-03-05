package de.uzl.its.swat.logger.inst;

/**
 * IF_ICMPLE - Branch if int comparison succeeds (less or equals) For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.if_icmp_cond">Java
 * VM specification</a>.
 */
public class IF_ICMPLE extends Instruction {
    // The jump destination
    int label;

    /**
     * Creates a new IF_ICMPLE instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param label the jump destination
     */
    public IF_ICMPLE(int iid, int mid, int label) {
        super(iid, mid);
        this.label = label;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitIF_ICMPLE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IF_ICMPLE " + label);
    }
}
