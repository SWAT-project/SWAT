package de.uzl.its.swat.logger.inst;

/**
 * IMUL - Multiply int.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.imul">Java VM specification</a>.
 */
public class IMUL extends Instruction {

    /**
     * Creates a new IMUL instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public IMUL(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitIMUL(this);
    }


    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IMUL");
    }
}