package de.uzl.its.swat.symbolic.instruction;

/**
 * NOP - Do nothing. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.nop">Java VM
 * specification</a>.
 */
public class NOP extends Instruction {

    /**
     * Creates a new NOP instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public NOP(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitNOP(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("NOP");
    }
}
