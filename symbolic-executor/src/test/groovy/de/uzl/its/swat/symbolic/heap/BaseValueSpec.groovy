package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.thread.ThreadHandler
import org.sosy_lab.java_smt.api.BooleanFormula
import org.sosy_lab.java_smt.api.FormulaManager
import org.sosy_lab.java_smt.api.ProverEnvironment
import org.sosy_lab.java_smt.api.SolverContext
import spock.lang.Specification

import static java.lang.Thread.currentThread

/**
 * Level L0 base: a clean Z3 {@link SolverContext} per test for constructing {@code Value} objects
 * and shadow structures directly (no instrumentation, no instruction stream). Mirrors the proven
 * {@code StringValueTest} setup. See docs/test-architecture.md (Level L0).
 *
 * Oracle helpers evaluate {@link BooleanFormula}s via SAT/UNSAT agreement with a real prover —
 * never by inspecting the formula's representation.
 */
abstract class BaseValueSpec extends Specification {

    protected SolverContext context
    protected FormulaManager fmgr

    def setup() {
        ThreadHandler.init()
        if (ThreadHandler.hasThreadContext(currentThread().id)) {
            ThreadHandler.removeThreadContext(currentThread().id)
        }
        ThreadHandler.addThreadContext(currentThread().id, "Test-Thread", -2)
        context = ThreadHandler.getSolverContext(currentThread().id)
        fmgr = context.getFormulaManager()
    }

    def cleanup() {
        if (ThreadHandler.hasThreadContext(currentThread().id)) {
            ThreadHandler.removeThreadContext(currentThread().id)
        }
    }

    /** True iff {@code f} is valid (always true): {@code not(f)} is unsatisfiable. */
    protected boolean isValid(BooleanFormula f) {
        ProverEnvironment p = context.newProverEnvironment()
        try {
            p.addConstraint(fmgr.getBooleanFormulaManager().not(f))
            return p.isUnsat()
        } finally {
            p.close()
        }
    }

    /** True iff {@code f} is unsatisfiable (always false). */
    protected boolean isUnsatisfiable(BooleanFormula f) {
        ProverEnvironment p = context.newProverEnvironment()
        try {
            p.addConstraint(f)
            return p.isUnsat()
        } finally {
            p.close()
        }
    }
}
