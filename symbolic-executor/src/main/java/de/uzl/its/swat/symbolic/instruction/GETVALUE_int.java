package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * GETVALUE_int - Custom method call handled as an instruction to fetch the concrete value that was
 * produced/ loaded by another instruction.
 */
public class GETVALUE_int extends GETVALUE_primitive {

    /**
     * Creates a new value fetch instruction for integers.
     *
     * @param v the concrete value
     * @param i symbolic marker (1 if symbolic else 0)
     */
    public GETVALUE_int(long iid, int v, int i) {
        super(iid, v, i);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitGETVALUE_int(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("I");
    }
}
