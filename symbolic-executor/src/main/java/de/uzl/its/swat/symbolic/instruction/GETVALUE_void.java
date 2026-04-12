package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * GETVALUE_void - Custom method call handled as an instruction to fetch the concrete value that was
 * produced/ loaded by another instruction.
 */
public class GETVALUE_void extends Instruction {

    /** Creates a new value fetch instruction for voids.
     *
     *  @param iid The unique instruction ID set during instrumentation.
     * */
    public GETVALUE_void(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
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
