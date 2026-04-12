package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * INVOKEMETHOD_EXCEPTION - Custom method handled as an instruction to signalize a method end. Used
 * to detect exceptions.
 */
public class INVOKEMETHOD_EXCEPTION extends InvokeIdInstruction {

    /**
     * Creates a new INVOKEMETHOD_EXCEPTION instruction.
     *
     * @param iid instruction id.
     * @param invokeId invoke id.
     */
    public INVOKEMETHOD_EXCEPTION(long iid, long invokeId) {
        super(iid, invokeId);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitINVOKEMETHOD_EXCEPTION(this);
    }

    @Override
    public String toString() {
        return "[METHOD EXCEPTION] ( " + invokeId + " )";
    }
}
