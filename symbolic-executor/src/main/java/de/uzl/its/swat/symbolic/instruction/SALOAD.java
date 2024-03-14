package de.uzl.its.swat.symbolic.instruction;

/**
 * SALOAD - Load short from array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.saload">Java VM
 * specification</a>.
 */
public class SALOAD extends Instruction {

    /**
     * Creates a new SALOAD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public SALOAD(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitSALOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("SALOAD");
    }
}
