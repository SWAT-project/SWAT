package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * FSTORE - Store float into local variable. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fstore">Java VM
 * specification</a>.
 */
public class FSTORE extends Instruction {
    // local variable index to store into
    public int var;

    /**
     * Creates a new FSTORE instruction.
     *
     * @param iid instruction id.
     * @param var local variable index to store into
     */
    public FSTORE(long iid, int var) {
        super(iid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitFSTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FSTORE " + var);
    }
}
