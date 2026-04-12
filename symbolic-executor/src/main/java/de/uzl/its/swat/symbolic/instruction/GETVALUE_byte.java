package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * GETVALUE_byte - Custom method call handled as an instruction to fetch the concrete value that was
 * produced/ loaded by another instruction.
 */
public class GETVALUE_byte extends GETVALUE_primitive {

    /**
     * Creates a new value fetch instruction for bytes.
     *
     * @param iid instruction id.
     * @param v the concrete value
     * @param i an identifier
     */
    public GETVALUE_byte(long iid, byte v, int i) {
        super(iid, v, i);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitGETVALUE_byte(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("B");
    }
}
