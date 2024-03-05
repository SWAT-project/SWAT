package de.uzl.its.swat.logger.inst;

/**
 * LUSHR - Logical shift right long.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lushr">Java VM specification</a>.
 */
public class LUSHR extends Instruction {

    /**
     * Creates a new LUSHR instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public LUSHR(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLUSHR(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LUSHR");
    }
}