package de.uzl.its.swat.symbolic.instruction;

/**
 * FSUB - Subtract float. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fsub">Java VM
 * specification</a>.
 */
public class FSUB extends Instruction {

    /**
     * Creates a new FSUB instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FSUB(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitFSUB(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FSUB");
    }
}
