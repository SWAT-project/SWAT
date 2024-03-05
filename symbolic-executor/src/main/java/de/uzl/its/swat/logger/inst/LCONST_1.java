package de.uzl.its.swat.logger.inst;

/**
 * LCONST_1 - Push long constant (1L).
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lconst_l">Java VM specification</a>.
 */
public class LCONST_1 extends Instruction {

    /**
     * Creates a new LCONST_1 instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public LCONST_1(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLCONST_1(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LCONST_1");
    }
}