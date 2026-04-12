package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * INVOKEMETHOD_END - Custom method handled as an instruction to signalize a method end. Signalizes
 * a method end.
 */
public class INVOKECLINIT_END extends InvokeIdInstruction {

    public final String name = "<clinit>";

    /**
     * Creates a new INVOKEMETHOD_END instruction.
     *
     * @param invokeId invoke id.
     */
    public INVOKECLINIT_END(long iid, long invokeId) {
        super(iid, invokeId);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitINVOKECLINIT_END(this);
    }

    @Override
    public String toString() {
        return "[CLINIT END]";
    }
}
