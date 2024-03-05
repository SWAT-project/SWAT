package de.uzl.its.swat.logger.inst;
/**
 * DUP2 - Duplicate the top one or two operand stack values.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dup2">Java VM specification</a>.
 */
public class DUP2 extends Instruction {

    /**
     * Creates a new DUP2 instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public DUP2(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor.
     */
    public void visit(IVisitor visitor) {
        visitor.visitDUP2(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DUP2");
    }
}
