package de.uzl.its.swat.logger.inst;

/**
 * LMUL - Multiply long.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lmul">Java VM specification</a>.
 */
public class LMUL extends Instruction {

    /**
     * Creates a new LMUL instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public LMUL(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLMUL(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LMUL");
    }
}