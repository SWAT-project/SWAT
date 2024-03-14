package de.uzl.its.swat.symbolic.instruction;

/**
 * LADD - Add long. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.ladd">Java VM
 * specification</a>.
 */
public class LADD extends Instruction {

    /**
     * Creates a new LADD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public LADD(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitLADD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LADD");
    }
}
