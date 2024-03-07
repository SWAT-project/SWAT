package de.uzl.its.swat.symbolic.instruction;

/**
 * DMUL - Convert double to float. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dmul">Java VM
 * specification</a>.
 */
public class DMUL extends Instruction {

    /**
     * Creates a new DMUL instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public DMUL(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitDMUL(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DMUL");
    }
}
