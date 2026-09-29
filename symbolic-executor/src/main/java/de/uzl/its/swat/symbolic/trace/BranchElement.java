package de.uzl.its.swat.symbolic.trace;

import static java.lang.Thread.currentThread;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.thread.ThreadHandler;
import lombok.Getter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaManager;

/** Represents a branch element in the symbolic trace. */
@Getter
final class BranchElement extends Element {

    // Indicates whether the branch was taken or not
    private final boolean branched;

    // The constraint of the branch
    private final BooleanFormula constraint;
    // A branch that only mirrors a guard the static pre-analysis graph has, e.g. the bounds check of
    // an array SWAT does not track. It keeps the explorer's walk of that graph in step, but has no
    // symbolic constraint, so the explorer never tries to flip it.
    private final boolean concreteOnly;

    /**
     * Constructs a branch element.
     *
     * @param branched Indicates whether the branch was taken or not
     * @param constraint The constraint of the branch
     * @param iid The unique identifier of the branch element derived from branch-instruction
     */
    BranchElement(boolean branched, BooleanFormula constraint, long iid) {
        this(branched, constraint, iid, false);
    }

    BranchElement(boolean branched, BooleanFormula constraint, long iid, boolean concreteOnly) {
        this.branched = branched;
        this.constraint = constraint;
        this.concreteOnly = concreteOnly;
        this.setIid(iid);
    }

    /** Private default constructor for serialization */
    private BranchElement() {
        this.branched = false;
        this.constraint = null;
        this.concreteOnly = false;
    }

    /**
     * Returns a string representation of the branch element.
     *
     * @return the representation of the branch element.
     */
    @Override
    public String toString() {
        FormulaManager fmgr;
        try {
            fmgr = ThreadHandler.getSolverContext(currentThread().getId()).getFormulaManager();
        } catch (NoThreadContextException e) {
            return "Branch ("
                    + branched
                    + ") "
                    + this.getIid();
        }
        return "Branch ("
                + branched
                + ") "
                + this.getIid()
                + " --> "
                + fmgr.dumpFormula(constraint);
    }
}
