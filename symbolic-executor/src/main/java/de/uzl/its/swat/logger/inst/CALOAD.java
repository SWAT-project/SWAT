package de.uzl.its.swat.logger.inst;

/**
 * CALOAD - Load char from array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.caload">Java VM
 * specification</a>.
 */
public class CALOAD extends Instruction {
    /**
     * Creates a new CALOAD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public CALOAD(int iid, int mid) {
        super(iid, mid);
    }
    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitCALOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("CALOAD");
    }
}
