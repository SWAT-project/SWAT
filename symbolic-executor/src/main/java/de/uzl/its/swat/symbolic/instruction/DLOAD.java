package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * DLOAD - Load double from local variable. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dload">Java VM
 * specification</a>.
 */
public class DLOAD extends Instruction {

    // The local variable index to load
    public int var;

    /**
     * Creates a new DLOAD instruction.
     *
     * @param iid instruction id.
     * @param var index of the local variable to load.
     */
    public DLOAD(long iid, int var) {
        super(iid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitDLOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DLOAD " + var);
    }
}
