package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * SUMMARY_JOIN - Probe at the label where the two sides of a merged diamond meet again (see
 * {@link de.uzl.its.swat.symbolic.summary.LoopSummaries}).
 */
public class SUMMARY_JOIN extends Instruction {

    /** The join's id (see LoopSummaries.Diamond.join). */
    public final int join;

    public SUMMARY_JOIN(long iid, int join) {
        super(iid);
        this.join = join;
    }

    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitSUMMARY_JOIN(this);
    }

    @Override
    public String toString() {
        return genericToString("[SUMMARY JOIN] " + join);
    }
}
