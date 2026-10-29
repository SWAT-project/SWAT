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
    // A branch the executor adds itself to enumerate the values of a symbolic operand (e.g. which
    // element of an object array a symbolic index selects). The static pre-analysis graph has no
    // such branch, so the explorer flips it but does not walk the graph over it.
    @lombok.Setter private boolean choice;

    // Where the branch is, for the explorer to check and resynchronise its walk of the static
    // pre-analysis graph (see BranchKeys): the branch's key, or null if its instruction has none,
    // and how the frame stack changed since the previous branch -- the number of frames popped,
    // then the call sites (keys, null if unknown) of the frames pushed.
    @lombok.Setter private String key;
    @lombok.Setter private int pops;
    @lombok.Setter private java.util.List<String> pushes = java.util.List.of();
    // Recorded where the static pre-analysis graph has no counterpart: in application code that
    // library code called back (see ShadowContext.inCallback), or in a SWAT helper such as the
    // reference comparison (see ShadowContext.inSwatHelper).
    @lombok.Setter private boolean callback;
    // The loop-head states the execution reached since the previous branch, in order (see
    // LoopStateMatcher).
    @lombok.Setter private java.util.List<String> loopStates = java.util.List.of();

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
