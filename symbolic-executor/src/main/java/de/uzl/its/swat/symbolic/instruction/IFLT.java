package de.uzl.its.swat.symbolic.instruction;

/**
 * IFLT - Branch if int comparison with zero succeeds (less than) For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.if_cond">Java VM
 * specification</a>.
 */
public class IFLT extends Instruction {
    // The jump destination
    int label;

    /**
     * Creates a new IFLT instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param label the jump destination
     */
    public IFLT(int iid, int mid, int label) {
        super(iid, mid);
        this.label = label;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitIFLT(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IFLT " + label);
    }
}
