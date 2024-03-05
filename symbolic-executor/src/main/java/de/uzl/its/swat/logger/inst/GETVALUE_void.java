package de.uzl.its.swat.logger.inst;

/**
 * GETVALUE_void - Custom method call handled as an instruction to fetch the concrete value that was
 * produced/ loaded by another instruction.
 */
public class GETVALUE_void extends Instruction {

    /** Creates a new value fetch instruction for voids. */
    public GETVALUE_void() {
        super(-1, -1);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_void(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return "[VALUE FETCH] V";
    }
}
