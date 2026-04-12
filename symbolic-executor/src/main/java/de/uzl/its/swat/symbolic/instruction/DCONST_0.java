package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * DCONST_0 - Push double (0.0d). For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dconst_d">Java VM
 * specification</a>.
 */
public class DCONST_0 extends Instruction {

    /**
     * Creates a new DCONST_0 instruction.
     *
     * @param iid instruction id.
     */
    public DCONST_0(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitDCONST_0(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DCONST_0");
    }
}
