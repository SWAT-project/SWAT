package de.uzl.its.swat.logger.inst;

/**
 * ICONST_2 - Push int constant (2). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.iconst_i">Java VM
 * specification</a>.
 */
public class ICONST_2 extends Instruction {

    /**
     * Creates a new ICONST_2 instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public ICONST_2(int iid, int mid) {
        super(iid, mid);
    }
    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitICONST_2(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ICONST_2");
    }
}
