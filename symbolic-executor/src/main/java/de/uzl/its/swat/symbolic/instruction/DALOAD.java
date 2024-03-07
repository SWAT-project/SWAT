package de.uzl.its.swat.symbolic.instruction;
/**
 * DALOAD - Load double from array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.daload">Java VM
 * specification</a>.
 */
public class DALOAD extends Instruction {

    /**
     * Creates a new DALOAD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public DALOAD(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitDALOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DALOAD");
    }
}
