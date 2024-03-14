package de.uzl.its.swat.symbolic.instruction;

/**
 * L2D - Convert long to double. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.l2d">Java VM
 * specification</a>.
 */
public class L2D extends Instruction {

    /**
     * Creates a new L2D instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public L2D(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitL2D(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("L2D");
    }
}
