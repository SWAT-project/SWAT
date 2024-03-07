package de.uzl.its.swat.symbolic.instruction;

/**
 * POP - Pop the top operand stack value. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.pop">Java VM
 * specification</a>.
 */
public class POP extends Instruction {

    /**
     * Creates a new POP instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public POP(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitPOP(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("POP");
    }
}
