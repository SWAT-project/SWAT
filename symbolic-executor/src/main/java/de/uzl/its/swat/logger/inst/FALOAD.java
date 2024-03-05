package de.uzl.its.swat.logger.inst;

/**
 * FALOAD - Load float from array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.faload">Java VM
 * specification</a>.
 */
public class FALOAD extends Instruction {

    /**
     * Creates a new FALOAD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FALOAD(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitFALOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FALOAD");
    }
}
