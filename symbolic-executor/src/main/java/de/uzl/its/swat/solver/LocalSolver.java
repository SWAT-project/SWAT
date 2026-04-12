package de.uzl.its.swat.solver;

import static java.lang.Thread.currentThread;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.PrintBox;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import de.uzl.its.swat.metadata.ClassDepot;
import de.uzl.its.swat.metadata.ClassDepotRuntime;
import de.uzl.its.swat.symbolic.trace.*;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.java_smt.api.*;

/**
 * This class can is used to directly communicate with the solver (currently Z3). The solver
 * interface only tries to find a solution for each branch (with the corresponding path constraints)
 * and log it. It cannot be used to perform iterative symbolic execution and is primarily intended
 * for debugging and testing purposes.
 */
public class LocalSolver {
    private static final Logger logger = GlobalLogger.getSolutionLogger();

    /**
     * Retrieves all recorded constraints and collects input bounds and path constraints for each
     * constraint. Then, it tries to find a solution for each constraint and logs it.
     */
    public static void solve() throws NoThreadContextException {
        logger.warn("Beginning to solve constraints.");
        SymbolicTraceHandler symbolicTraceHandler =
                ThreadHandler.getSymbolicVisitor(currentThread().getId()).getSymbolicTraceHandler();
        FormulaManager fmgr =
                ThreadHandler.getSolverContext(currentThread().getId()).getFormulaManager();
        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();

        // Logs all constraints
        // symbolicTraceHandler.dumpConstraints(logger.getLogger());

        HashMap<Long, BooleanFormula> constraints = symbolicTraceHandler.getBranchConstraints();
        logger.warn("Found " + constraints.size() + " constraints.");


        ClassDepotRuntime classDepot = ClassDepot.getRuntimeInstance();
        int nullcnt = 0;
        int ooscnt = 0;
        // Iterates over all constraints and tries to find a solution for each
        for (Map.Entry<Long, BooleanFormula> entry : constraints.entrySet()) {


            long iid = entry.getKey();
            int cid = (int) GlobalStateForInstrumentation.extractCid(iid);
            if (cid >= ClassDepot.getRuntimeInstance().getClassCounter()) {
                new ErrorHandler().raiseException("cid " + cid + " larger than class count "
                        + ClassDepot.getRuntimeInstance().getClassCounter());
            }

            String cname = classDepot.getClassName(cid);
            if (cname == null){
                nullcnt++;
                continue;
            } else if (!Util.isInSymbolicScope(cname)) {
                ooscnt++;
                continue;
            }
            // The constraint is negated to find a solution leading the opposite branching
            // direction.
            BooleanFormula constraint = bmgr.not(entry.getValue());
            logger.info("Processing constraint for class {}: {}", cname, constraint);


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
                    HashMap<Long, BooleanFormula> pathConstraints =
                            symbolicTraceHandler.getPathConstraints(entry.getKey());
                    for (BooleanFormula pathConstraint : pathConstraints.values()) {
                        prover.addConstraint(pathConstraint);
                    }

                    // Add the actual constraint
                    prover.addConstraint(constraint);

                    boolean isUnsat = prover.isUnsat();
                    PrintBox printBox = new PrintBox(60, "Solution (LocalSolver)");
                    printBox.addMsg(
                            "[Endpoint] " + ThreadHandler.getEndpointName(currentThread().getId()));
                    ArrayList<String> inputs = symbolicTraceHandler.dumpInputs();
                    for (String input : inputs) {
                        printBox.addMsg(input);
                    }
                    printBox.addMsg(
                            "[Constraint] "
                                    + fmgr.dumpFormula(constraint)
                                            .toString()
                                            .replaceAll("[\\r\\n]", ""));
                    if (!isUnsat) {
                        Model model = prover.getModel();

                        printBox.addMsg("[Model] " + model);
                    } else {
                        printBox.addMsg("[Model] UNSAT");
                    }
                    logger.warn(printBox.toString());
                } catch (Throwable t) {
                    new ErrorHandler().handleException("Error while solving constraints", t);
                }
            } else {
                logger.warn("Skipping constraint without free variables");
            }
        }
        logger.info("Skipped {} constraints with null class names.", nullcnt);
        logger.info("Skipped {} constraints with out-of-scope class names.", ooscnt);
    }
}
