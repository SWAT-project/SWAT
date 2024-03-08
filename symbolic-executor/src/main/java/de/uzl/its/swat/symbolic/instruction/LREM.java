package de.uzl.its.swat.symbolic.instruction;

/**
 * LREM - Remainder long. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lrem">Java VM
 * specification</a>.
 */
public class LREM extends Instruction {

    /**
     * Creates a new LREM instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public LREM(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitLREM(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LREM");
    }
}