package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * CHECKCAST - Check whether object is of given type. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.checkcast">Java VM
 * specification</a>.
 */
public class CHECKCAST extends Instruction {
    // The type to check against
    String type;

    /**
     * Creates a new CHECKCAST instruction.
     *
     * @param iid instruction id.
     * @param type the type to check against
     */
    public CHECKCAST(long iid, String type) {
        super(iid);
        this.type = type;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitCHECKCAST(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("CHECKCAST " + type);
    }
}
