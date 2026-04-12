package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * ICONST_M1 - Push int constant (-1). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.iconst_i">Java VM
 * specification</a>.
 */
public class ICONST_M1 extends Instruction {

    /**
     * Creates a new ICONST_M1 instruction.
     *
     * @param iid instruction id.
     */
    public ICONST_M1(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitICONST_M1(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ICONST_M1");
    }
}
