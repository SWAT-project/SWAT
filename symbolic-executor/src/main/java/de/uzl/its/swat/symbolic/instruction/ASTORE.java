package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;
/**
 * ASTORE -Store reference into local variable. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.astore">Java VM
 * specification</a>.
 */
public class ASTORE extends Instruction {

    // Index of the local variable where the reference is stored.
    public int var;

    /**
     * Creates a new ASTORE instruction.
     *
     * @param iid instruction id.
     * @param var index of the local variable where the reference is stored.
     */
    public ASTORE(long iid, int var) {
        super(iid);
        this.var = var;
    }
    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitASTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ASTORE " + var);
    }
}
