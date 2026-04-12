package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * ICONST_0 - Push int constant (0). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.iconst_i">Java VM
 * specification</a>.
 */
public class ICONST_0 extends Instruction {

    /**
     * Creates a new ICONST_0 instruction.
     *
     * @param iid instruction id.
     */
    public ICONST_0(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitICONST_0(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ICONST_0");
    }
}
