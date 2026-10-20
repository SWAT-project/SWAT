package de.uzl.its.swat.symbolic.trace;

import ch.qos.logback.classic.Logger;
import com.microsoft.z3.Native;
import de.uzl.its.swat.common.logging.GlobalLogger;
import java.lang.reflect.Method;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.basicimpl.AbstractFormulaManager;
import org.sosy_lab.java_smt.basicimpl.FormulaCreator;

/**
 * Dumps formulas as SMT-LIB like {@link FormulaManager#dumpFormula}, with the same output, but
 * faster for many formulas. For Z3, java-smt creates a new solver for every formula, asserts the
 * formula in it and prints the solver, and initialising that solver costs more than printing a
 * small formula. This dumper keeps one solver and asserts each formula in its own scope.
 *
 * <p>The Z3 handles are only reachable through protected java-smt methods, so they are looked up
 * reflectively; if that fails (another java-smt version or solver), every formula goes through
 * {@link FormulaManager#dumpFormula} instead.
 *
 * <p>The dumper of the last formula manager is kept: there is one per solver context, and one
 * context per thread. Its solver lives as long as the context.
 */
final class FormulaDumper {
    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private final FormulaManager fmgr;
    private final FormulaCreator<?, ?, ?, ?> creator;
    private final Method extractInfo;
    private final Method sanitize;
    private final long context;
    private final long solver;

    private FormulaDumper(
            FormulaManager fmgr,
            FormulaCreator<?, ?, ?, ?> creator,
            Method extractInfo,
            Method sanitize,
            long context,
            long solver) {
        this.fmgr = fmgr;
        this.creator = creator;
        this.extractInfo = extractInfo;
        this.sanitize = sanitize;
        this.context = context;
        this.solver = solver;
    }

    private static volatile FormulaDumper last;

    /** The dumper for the formulas of the given manager. */
    static FormulaDumper of(FormulaManager fmgr) {
        FormulaDumper dumper = last;
        if (dumper == null || dumper.fmgr != fmgr) {
            dumper = create(fmgr);
            last = dumper;
        }
        return dumper;
    }

    /** A dumper for the formulas of the given manager; one that falls back if Z3 is not reachable. */
    private static FormulaDumper create(FormulaManager fmgr) {
        try {
            if (fmgr instanceof AbstractFormulaManager<?, ?, ?, ?> afm
                    && afm.getFormulaCreator().getEnv() instanceof Long context
                    && afm.getClass().getSimpleName().equals("Z3FormulaManager")) {
                Method extractInfo = FormulaCreator.class.getDeclaredMethod("extractInfo", Formula.class);
                extractInfo.setAccessible(true);
                Method sanitize = AbstractFormulaManager.class.getDeclaredMethod("sanitize", String.class);
                sanitize.setAccessible(true);
                long solver = Native.mkSolver(context);
                Native.solverIncRef(context, solver);
                return new FormulaDumper(fmgr, afm.getFormulaCreator(), extractInfo, sanitize, context, solver);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.debug("Dumping formulas through java-smt: {}", e.toString());
        }
        return new FormulaDumper(fmgr, null, null, null, 0, 0);
    }

    /** The formula as SMT-LIB: its declarations and one assert, as {@link FormulaManager#dumpFormula}. */
    String dump(BooleanFormula f) {
        if (creator == null) {
            return String.valueOf(fmgr.dumpFormula(f));
        }
        try {
            long expr = (Long) extractInfo.invoke(creator, f);
            String dumped;
            Native.solverPush(context, solver);
            try {
                Native.solverAssert(context, solver, expr);
                dumped = Native.solverToString(context, solver);
            } finally {
                Native.solverPop(context, solver, 1);
            }
            return (String) sanitize.invoke(fmgr, dumped);
        } catch (ReflectiveOperationException e) {
            return String.valueOf(fmgr.dumpFormula(f));
        }
    }
}
