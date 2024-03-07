package de.uzl.its.swat.symbolic.instruction;

/**
 * FREM - Remainder float. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.frem">Java VM
 * specification</a>.
 */
public class FREM extends Instruction {

    /**
     * Creates a new FREM instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FREM(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitFREM(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FREM");
    }
}
