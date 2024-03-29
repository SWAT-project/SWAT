package de.uzl.its.swat.symbolic.instruction;

/**
 * LCONST_0 - Push long constant (0L). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lconst_l">Java VM
 * specification</a>.
 */
public class LCONST_0 extends Instruction {

    /**
     * Creates a new LCONST_0 instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public LCONST_0(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitLCONST_0(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LCONST_0");
    }
}
