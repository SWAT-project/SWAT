package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/** LOOP_END - Custom method call handled as an instruction to detect the end of loops. */
public class LOOP_END extends Instruction {

    /**
     * Creates a new loop end marker instruction.
     *
     * @param iid instruction id.
     */
    public LOOP_END(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitLOOP_END(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("[LOOP END]");
    }
}
