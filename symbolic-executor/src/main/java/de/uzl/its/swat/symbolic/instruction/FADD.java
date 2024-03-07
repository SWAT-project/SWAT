package de.uzl.its.swat.symbolic.instruction;

/**
 * FADD - Add float. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fadd">Java VM
 * specification</a>.
 */
public class FADD extends Instruction {

    /**
     * Creates a new FADD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FADD(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitFADD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FADD");
    }
}
