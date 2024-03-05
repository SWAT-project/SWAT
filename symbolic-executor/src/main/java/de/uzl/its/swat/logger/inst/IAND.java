package de.uzl.its.swat.logger.inst;

/**
 * IAND - Boolean AND int. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.iand">Java VM
 * specification</a>.
 */
public class IAND extends Instruction {

    /**
     * Creates a new IAND instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public IAND(int iid, int mid) {
        super(iid, mid);
    }
    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitIAND(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IAND");
    }
}
