package de.uzl.its.swat.logger.inst;

/**
 * LOOP_BEGIN - Custom method call handled as an instruction to detect the beginning of loops.
 */
public class LOOP_BEGIN extends Instruction {

    /**
     * Creates a new loop beginning marker instruction.
     * @param iid instruction id.
     */
    public LOOP_BEGIN(int iid) {
        super(iid, -1);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLOOP_BEGIN(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("[LOOP BEGIN]");
    }
}