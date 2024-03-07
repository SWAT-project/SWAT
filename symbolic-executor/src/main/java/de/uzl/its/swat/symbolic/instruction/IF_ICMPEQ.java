package de.uzl.its.swat.symbolic.instruction;

/**
 * IF_ICMPEQ - Branch if int comparison succeeds (equals) For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.if_icmp_cond">Java
 * VM specification</a>.
 */
public class IF_ICMPEQ extends Instruction {

    // The jump destination
    int label;

    /**
     * Creates a new IF_ICMPEQ instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param label the jump destination
     */
    public IF_ICMPEQ(int iid, int mid, int label) {
        super(iid, mid);
        this.label = label;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitIF_ICMPEQ(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IF_ICMPEQ " + label);
    }
}
