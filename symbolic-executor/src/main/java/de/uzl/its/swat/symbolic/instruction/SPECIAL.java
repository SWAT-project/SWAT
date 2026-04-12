package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * A special probe instruction added by the instrumentation. The int value identifies which branch
 * does the instruction correspond to.
 */
public class SPECIAL extends Instruction {

    // Marker used in symbolic interpreter for example for branch detection
    public int i;

    /**
     * Creates a new SPECIAL instruction.
     *
     * @param i marker value
     */
    public SPECIAL(long iid, int i) {
        super(iid);
        this.i = i;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitSPECIAL(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("[SPECIAL] " + i);
    }
}
