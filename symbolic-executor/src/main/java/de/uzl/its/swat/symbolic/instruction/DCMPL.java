package de.uzl.its.swat.symbolic.instruction;
/**
 * DCMPL - Compare double (less than). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dcmpl">Java VM
 * specification</a>.
 */
public class DCMPL extends Instruction {

    /**
     * Creates a new DCMPL instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public DCMPL(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitDCMPL(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DCMPL");
    }
}
