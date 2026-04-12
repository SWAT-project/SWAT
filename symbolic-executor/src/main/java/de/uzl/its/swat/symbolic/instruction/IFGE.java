package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * IFGE - Branch if int comparison with zero succeeds (greater or equal) For more information see
 * the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.if_cond">Java VM
 * specification</a>.
 */
public class IFGE extends Instruction {
    // The jump destination
    int label;

    /**
     * Creates a new IFGE instruction.
     *
     * @param iid instruction id.
     * @param label the jump destination
     */
    public IFGE(long iid, int label) {
        super(iid);
        this.label = label;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitIFGE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IFGE " + label);
    }
}
