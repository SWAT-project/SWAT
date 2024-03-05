package de.uzl.its.swat.logger.inst;
/**
 * BALOAD - Load byte or boolean from array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.baload">Java VM
 * specification</a>.
 */
public class BALOAD extends Instruction {
    /**
     * Creates a new BALOAD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public BALOAD(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitBALOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("BALOAD");
    }
}
