package de.uzl.its.swat.symbolic.instruction;

/**
 * GOTO - Branch always. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.goto">Java VM
 * specification</a>.
 */
public class GOTO extends Instruction {

    // The jump label
    int label;

    /**
     * Creates a new GOTO instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param label the jump label
     */
    public GOTO(int iid, int mid, int label) {
        super(iid, mid);
        this.label = label;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitGOTO(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("GOTO " + label);
    }
}
