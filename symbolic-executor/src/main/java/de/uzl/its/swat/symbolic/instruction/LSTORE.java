package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * LSTORE - Store long into local variable. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lstore">Java VM
 * specification</a>.
 */
public class LSTORE extends Instruction {

    // Local variable index
    public int var;

    /**
     * Creates a new LSTORE instruction.
     *
     * @param iid instruction id.
     * @param var local variable index.
     */
    public LSTORE(long iid, int var) {
        super(iid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitLSTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LSTORE " + var);
    }
}
