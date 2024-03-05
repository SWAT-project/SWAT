package de.uzl.its.swat.logger.inst;

/**
 * DUP - Duplicate the top operand stack value. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dup">Java VM
 * specification</a>.
 */
public class DUP extends Instruction {

    /**
     * Creates a new DUP instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public DUP(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitDUP(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DUP");
    }
}
