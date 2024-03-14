package de.uzl.its.swat.symbolic.instruction;

/**
 * LAND - Boolean AND long. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.land">Java VM
 * specification</a>.
 */
public class LAND extends Instruction {

    /**
     * Creates a new LAND instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public LAND(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitLAND(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LAND");
    }
}
