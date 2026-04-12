package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/** LOOP_BEGIN - Custom method call handled as an instruction to detect the beginning of loops. */
public class LOOP_BEGIN extends Instruction {

    /**
     * Creates a new loop beginning marker instruction.
     *
     * @param iid instruction id.
     */
    public LOOP_BEGIN(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitLOOP_BEGIN(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("[LOOP BEGIN]");
    }
}
