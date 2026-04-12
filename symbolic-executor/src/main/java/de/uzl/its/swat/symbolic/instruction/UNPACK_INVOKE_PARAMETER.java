package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

public class UNPACK_INVOKE_PARAMETER extends Instruction {

    public UNPACK_INVOKE_PARAMETER(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitUNPACK_INVOKE_PARAMETER(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {

        // return genericToString("CLINIT " + type + " " + cIdx);
        return genericToString("UNPACK_INVOKE_PARAMETER " + " " + iid);
    }
}
