package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * The start of a catch handler of the instrumented code. {@code key} names the handler (see
 * {@code HandlerKeys}) so that the static pre-analysis walk can resume there after an exception
 * it does not model.
 */
public class HANDLER_ENTRY extends Instruction {
    public final String key;

    public HANDLER_ENTRY(long iid, String key) {
        super(iid);
        this.key = key;
    }

    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitHANDLER_ENTRY(this);
    }

    @Override
    public String toString() {
        return genericToString("HANDLER_ENTRY " + key);
    }
}
