package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * LOOP_CALL - Probe before a call whose caller state can be hashed while the callee runs, carrying
 * the concrete values of the caller's locals that are live after the call (see {@link
 * de.uzl.its.swat.symbolic.loopstate.LoopHeadProbes}). It comes before the call's INVOKE probe, so
 * the caller's frame is still the active one when it is processed.
 */
public class LOOP_CALL extends Instruction {

    /** The call site's stable name: class, method and position (see LoopHeadProbes). */
    public final String site;

    /** The iid of the call's INVOKE probe: the call site of the frame the call pushes. */
    public final long invoke;

    /** The values of the live locals, in the order LoopHeadProbes.Call lists their slots. */
    public final Object[] locals;

    public LOOP_CALL(long iid, String site, long invoke, Object[] locals) {
        super(iid);
        this.site = site;
        this.invoke = invoke;
        this.locals = locals;
    }

    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitLOOP_CALL(this);
    }

    @Override
    public String toString() {
        return genericToString("[LOOP CALL] " + site);
    }
}
