package de.uzl.its.swat.symbolic.instruction;

/**
 * L2I - Convert long to int. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.l2i">Java VM
 * specification</a>.
 */
public class L2I extends Instruction {

    /**
     * Creates a new L2I instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public L2I(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitL2I(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("L2I");
    }
}
