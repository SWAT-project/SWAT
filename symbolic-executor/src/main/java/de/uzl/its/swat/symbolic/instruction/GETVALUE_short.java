package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * GETVALUE_short - Custom method call handled as an instruction to fetch the concrete value that
 * was produced/ loaded by another instruction.
 */
public class GETVALUE_short extends GETVALUE_primitive {

    /**
     * Creates a new value fetch instruction for shorts.
     *
     * @param iid The unique instruction ID set during instrumentation.
     * @param v the concrete value
     * @param i an identifier
     */
    public GETVALUE_short(long iid, short v, int i) {
        super(iid, v, i);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitGETVALUE_short(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("S");
    }
}
