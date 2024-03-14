package de.uzl.its.swat.symbolic.instruction;

/**
 * D2F - Convert double to long. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.d2l">Java VM
 * specification</a>.
 */
public class D2L extends Instruction {

    /**
     * Creates a new D2L instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public D2L(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitD2L(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("D2L");
    }
}
