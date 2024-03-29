package de.uzl.its.swat.symbolic.instruction;

/**
 * LDC(_double) - Push item from run-time constant pool. LDC instructions are handled based on the
 * datatype to load. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lconst_l">Java VM
 * specification</a>.
 */
public class LDC_double extends Instruction {

    // The value to load
    public double c;

    /**
     * Creates a new LDC_double instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param c the value to load
     */
    public LDC_double(int iid, int mid, double c) {
        super(iid, mid);
        this.c = c;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitLDC_double(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LDC [D] " + c);
    }
}
