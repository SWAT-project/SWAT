package de.uzl.its.swat.symbolic.trace;

import static java.lang.Thread.currentThread;

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

    /**
     * Constructs a branch element.
     *
     * @param branched Indicates whether the branch was taken or not
     * @param constraint The constraint of the branch
     * @param iid The unique identifier of the branch element derived from branch-instruction
     */
    BranchElement(boolean branched, BooleanFormula constraint, int iid) {
        this.branched = branched;
        this.constraint = constraint;
        this.setIid(iid);
    }

    /** Private default constructor for serialization */
    private BranchElement() {
        this.branched = false;
        this.constraint = null;
    }

    /**
     * Returns a string representation of the branch element.
     *
     * @return the representation of the branch element.
     */
    @Override
    public String toString() {
        FormulaManager fmgr =
                ThreadHandler.getSolverContext(currentThread().getId()).getFormulaManager();
        return "Branch ("
                + branched
                + ") "
                + this.getIid()
                + " --> "
                + fmgr.dumpFormula(constraint);
    }
}
