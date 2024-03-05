package de.uzl.its.swat.logger.inst;

/**
 * LOOP_END - Custom method call handled as an instruction to detect the end of loops.
 */
public class LOOP_END extends Instruction {

    /**
     * Creates a new loop end marker instruction.
     * @param iid instruction id.
     */
    public LOOP_END(int iid) {
        super(iid, -1);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLOOP_END(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("[LOOP END]");
    }
}