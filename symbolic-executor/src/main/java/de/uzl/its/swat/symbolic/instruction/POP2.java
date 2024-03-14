package de.uzl.its.swat.symbolic.instruction;

/**
 * POP2 - Pop the top one or two operand stack values. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.pop2">Java VM
 * specification</a>.
 */
public class POP2 extends Instruction {

    /**
     * Creates a new POP2 instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public POP2(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitPOP2(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("POP2");
    }
}
