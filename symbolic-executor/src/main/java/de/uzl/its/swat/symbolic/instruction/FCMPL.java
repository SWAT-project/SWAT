package de.uzl.its.swat.symbolic.instruction;

/**
 * FCMPL - Compare float (less than). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fcmp_op">Java VM
 * specification</a>.
 */
public class FCMPL extends Instruction {

    /**
     * Creates a new FCMPL instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FCMPL(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitFCMPL(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FCMPL");
    }
}
