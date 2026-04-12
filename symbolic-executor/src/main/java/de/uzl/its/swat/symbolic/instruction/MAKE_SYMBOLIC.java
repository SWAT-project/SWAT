package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * MAKE_SYMBOLIC - Custom method call handled as an instruction to initiate symbolic tracking of
 * variables.
 */
public class MAKE_SYMBOLIC extends Instruction {

    /** Creates a new symbolic marker instruction.
     *
     * @param iid instruction id.
     */
    public MAKE_SYMBOLIC(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitMAKE_SYMBOLIC(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("[MAKE SYMBOLIC]");
    }
}
