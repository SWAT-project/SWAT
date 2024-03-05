package de.uzl.its.swat.logger.inst;

/**
 * DUP_X1 - Duplicate the top operand stack value and insert two values down. For more information
 * see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dup_x1">Java VM
 * specification</a>.
 */
public class DUP_X1 extends Instruction {

    /**
     * Creates a new DUP_X1 instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public DUP_X1(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitDUP_X1(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DUP_X1");
    }
}
