package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * LOOP_HEAD - Probe at the target of a backward jump, carrying the concrete values of the locals
 * defined there (see {@link de.uzl.its.swat.symbolic.loopstate.LoopHeads}).
 */
public class LOOP_HEAD extends Instruction {

    /** The loop head's stable name: class, method and position (see LoopHeads). */
    public final String head;

    /** The values of the head's locals, in the order LoopHeads lists their slots. */
    public final Object[] locals;

    public LOOP_HEAD(long iid, String head, Object[] locals) {
        super(iid);
        this.head = head;
        this.locals = locals;
    }

    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitLOOP_HEAD(this);
    }

    @Override
    public String toString() {
        return genericToString("[LOOP HEAD] " + head);
    }
}
