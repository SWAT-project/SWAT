package de.uzl.its.swat.symbolic.instruction;
/**
 * D2I - Convert double to int. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.d2i">Java VM
 * specification</a>.
 */
public class D2I extends Instruction {
    /**
     * Creates a new D2I instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public D2I(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitD2I(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("D2I");
    }
}
