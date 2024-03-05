package de.uzl.its.swat.logger.inst;

/**
 * IUSHR - Logical shift right int. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.iushr">Java VM
 * specification</a>.
 */
public class IUSHR extends Instruction {

    /**
     * Creates a new IUSHR instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public IUSHR(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitIUSHR(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IUSHR");
    }
}
