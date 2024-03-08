package de.uzl.its.swat.solver;

import static java.lang.Thread.currentThread;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.symbolic.trace.*;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosy_lab.java_smt.api.*;

/**
 * This class can is used to directly communicate with the solver (currently Z3). The solver
 * interface only tries to find a solution for each branch (with the corresponding path constraints)
 * and log it. It cannot be used to perform iterative symbolic execution and is primarily intended
 * for debugging and testing purposes.
 */
public class LocalSolver {
    private static final Logger logger = LoggerFactory.getLogger(LocalSolver.class);

    /**
     * Retrieves all recorded constraints and collects input bounds and path constraints for each
     * constraint. Then, it tries to find a solution for each constraint and logs it.
     */
    public static void solve() {
        logger.debug("Beginning to solve constraints.");
        SymbolicTraceHandler symbolicTraceHandler =
                ThreadHandler.getSymbolicVisitor(currentThread().getId()).getSymbolicStateHandler();
        FormulaManager fmgr =
                ThreadHandler.getSolverContext(currentThread().getId()).getFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();

        // Logs all constraints
        // symbolicTraceHandler.dumpConstraints(logger.getLogger());

        HashMap<Integer, BooleanFormula> constraints = symbolicTraceHandler.getBranchConstraints();
        logger.debug("Found " + constraints.size() + " constraints.");
        HashSet<String> solutions = new HashSet<>();

        // Iterates over all constraints and tries to find a solution for each
        for (Map.Entry<Integer, BooleanFormula> entry : constraints.entrySet()) {

            // The constraint is negated to find a solution leading the opposite branching
            // direction.
            BooleanFormula constraint = bmgr.not(entry.getValue());

            Map<String, Formula> freeVars = fmgr.extractVariablesAndUFs(constraint);
            // Only solve for constraints that contain symbolic variables (free variables)
            if (!freeVars.isEmpty()) {
                try (ProverEnvironment prover =
                        ThreadHandler.getSolverContext(currentThread().getId())
                                .newProverEnvironment(
                                        SolverContext.ProverOptions.GENERATE_MODELS)) {

                    // Add input bounds
                    for (BooleanFormula inputConstraint : symbolicTraceHandler.getInputBounds()) {
                        prover.addConstraint(inputConstraint);
                    }

                    // Add path constraints
                    HashMap<Integer, BooleanFormula> pathConstraints =
                            symbolicTraceHandler.getPathConstraints(entry.getKey());
                    for (BooleanFormula pathConstraint : pathConstraints.values()) {
                        prover.addConstraint(pathConstraint);
                    }

                    // Add the actual constraint
                    prover.addConstraint(constraint);

                    boolean isUnsat = prover.isUnsat();

                    logger.info("--------------------------------------------------------------");
                    logger.info("------------------------- Solution ---------------------------");
                    logger.info("--------------------------------------------------------------");
                    logger.info(
                            "[Endpoint] " + ThreadHandler.getEndpointName(currentThread().getId()));
                    symbolicTraceHandler.dumpInputs(logger);
                    logger.info(
                            "[Constraint] "
                                    + fmgr.dumpFormula(constraint)
                                            .toString()
                                            .replaceAll("[\\r\\n]", ""));
                    if (!isUnsat) {
                        Model model = prover.getModel();
                        String sol = String.valueOf(model.asList());
                        logger.info("[Model] " + sol);
                    } else {
                        logger.info("[Model] UNSAT");
                    }
                    logger.info("--------------------------------------------------------------");
                } catch (Throwable t) {
                    new ErrorHandler().handleException("Error while solving constraints", t);
                }
            }
        }
    }
}
