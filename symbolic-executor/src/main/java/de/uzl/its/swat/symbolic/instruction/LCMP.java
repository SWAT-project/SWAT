package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * LCMP - Compare long. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lcmp">Java VM
 * specification</a>.
 */
public class LCMP extends Instruction {

    /**
     * Creates a new LCMP instruction.
     *
     * @param iid instruction id.
     */
    public LCMP(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitLCMP(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LCMP");
    }
}
