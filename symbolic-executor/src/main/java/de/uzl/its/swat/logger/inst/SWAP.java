package de.uzl.its.swat.logger.inst;

/**
 * SWAP - Swap the top two operand stack values. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.swap">Java VM
 * specification</a>.
 */
public class SWAP extends Instruction {

    /**
     * Creates a new SWAP instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public SWAP(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitSWAP(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("SWAP");
    }
}
