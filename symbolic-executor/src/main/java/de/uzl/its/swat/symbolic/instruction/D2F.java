package de.uzl.its.swat.symbolic.instruction;

/**
 * D2F - Convert double to float. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.d2f">Java VM
 * specification</a>.
 */
public class D2F extends Instruction {

    /**
     * Creates a new D2F instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public D2F(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitD2F(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("D2F");
    }
}
