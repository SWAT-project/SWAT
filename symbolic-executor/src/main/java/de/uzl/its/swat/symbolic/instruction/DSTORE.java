package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * DSTORE - Store double into local variable. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dstore">Java VM
 * specification</a>.
 */
public class DSTORE extends Instruction {

    // Index of the local variable where the double is stored.
    public int var;

    /**
     * Creates a new DSTORE instruction.
     *
     * @param iid instruction id.
     * @param var index of the local variable where the double is stored.
     */
    public DSTORE(long iid, int var) {
        super(iid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitDSTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DSTORE " + var);
    }
}
