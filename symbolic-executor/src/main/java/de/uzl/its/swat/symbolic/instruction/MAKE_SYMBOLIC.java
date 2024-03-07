package de.uzl.its.swat.symbolic.instruction;

/**
 * MAKE_SYMBOLIC - Custom method call handled as an instruction to initiate symbolic tracking of
 * variables.
 */
public class MAKE_SYMBOLIC extends Instruction {

    /** Creates a new symbolic marker instruction. */
    public MAKE_SYMBOLIC() {
        super(-1, -1);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
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
