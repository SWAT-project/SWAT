package de.uzl.its.swat.logger.inst;

/**
 * LOR - Boolean OR long. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lor">Java VM
 * specification</a>.
 */
public class LOR extends Instruction {

    /**
     * Creates a new LOR instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public LOR(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLOR(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LOR");
    }
}
