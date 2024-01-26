package de.uzl.its.swat.solver;

import static java.lang.Thread.currentThread;

import de.uzl.its.swat.executionData.SymbolicStateHandler;
import de.uzl.its.symbolic.BranchElement;
import de.uzl.its.symbolic.Element;
import de.uzl.its.symbolic.InputElement;
import de.uzl.its.swat.thread.ThreadHandler;
import java.util.HashSet;
import java.util.Map;
import org.sosy_lab.java_smt.api.*;

public class LocalSolver {

    public static void solve() {
        SymbolicStateHandler symbolicStateHandler =
                ThreadHandler.getConcolicInterpreter(currentThread().getId())
                        .getSymbolicStateHandler();
        FormulaManager fmgr =
                ThreadHandler.getSolverContext(currentThread().getId()).getFormulaManager();

        BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();

        System.out.println("-------------------- Dumping all constraints --------------------");
        int cnt = 0;
        for (Element elem : symbolicStateHandler.getExecutionData().getTrace()) {
            if (elem instanceof BranchElement b) {
                System.out.println("   " + cnt + " --> " + fmgr.dumpFormula(b.getPathConstraint()));
                cnt += 1;
            }
        }

        HashSet<String> solutions = new HashSet<>();
        cnt = 0;
        for (int i = 0; i < symbolicStateHandler.getExecutionData().getTrace().size(); i++) {

            Element e = symbolicStateHandler.getExecutionData().getTrace().get(i);
            if (e instanceof BranchElement b) {

                System.out.println(
                        "--------------------------------------------------------------------------");
                System.out.println(
                        "-------------------- Solving for constraint: "
                                + cnt
                                + " ----------------------------");
                System.out.println(
                        "--------------------------------------------------------------------------");
                System.out.println(e);
                BooleanFormula constraint = bmgr.not(b.getPathConstraint());
                System.out.println(fmgr.dumpFormula(constraint));
                System.out.println(
                        "--------------------------- Input constraints:"
                                + " ---------------------------");
                Map<String, Formula> freeVars = fmgr.extractVariablesAndUFs(constraint);
                if (!freeVars.isEmpty()) {
                    try (ProverEnvironment prover =
                                 ThreadHandler.getSolverContext(currentThread().getId())
                                         .newProverEnvironment(
                                                 SolverContext.ProverOptions.GENERATE_MODELS)) {
                        String inputs = "Inputs: [";
                        for (InputElement input :
                                symbolicStateHandler.getExecutionData().getInputs()) {
                            BooleanFormula lowerBound = input.getValue().getBounds(false);
                            BooleanFormula upperBound = input.getValue().getBounds(true);
                            prover.addConstraint(lowerBound);
                            prover.addConstraint(upperBound);
                            System.out.println(fmgr.dumpFormula(lowerBound));
                            System.out.println(fmgr.dumpFormula(upperBound));
                            inputs =
                                    inputs.concat(
                                            input.getName()
                                                    + ", "
                                                    + input.getValue().getConcrete()
                                                    + ", ");
                        }
                        inputs = inputs.substring(0, inputs.length() - 2);
                        inputs = inputs.concat("]");
                        System.out.println(
                                "--------------------------- Additional Path constraints:"
                                        + " ---------------------------");

                        for (int k = 0; k < i; k++) {
                            Element el = symbolicStateHandler.getExecutionData().getTrace().get(k);
                            if (el instanceof BranchElement be) {
                                prover.addConstraint(be.getPathConstraint());
                                System.out.println(fmgr.dumpFormula(be.getPathConstraint()));
                            }
                        }
                        prover.addConstraint(constraint);
                        // prover.addConstraint(b2);
                        boolean isUnsat = prover.isUnsat();
                        if (!isUnsat) {
                            Model model = prover.getModel();
                            String sol = String.valueOf(model.asList());
                            if (!solutions.contains(sol)) {
                                solutions.add(inputs + " -> Solutions: " + sol);
                                System.out.println(
                                        "-------------------- Dumping"
                                                + " Solution--------------------");
                                System.out.println("-------------------- Model ---------------");
                                System.out.println(model);
                                ;
                            } else {
                                System.out.println("Not a new solution");
                            }
                        } else {
                            System.out.println("UNSAT");
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
        System.out.println("======== BEGIN OF OUTPUT FOR AUTOMATIC VALIDATION ========");
        for (String sol : solutions) {
            System.out.println(
                    ThreadHandler.getEndpointName(currentThread().getId()) + " :: " + sol);
        }
        System.out.println("========= END OF OUTPUT FOR AUTOMATIC VALIDATION =========");
    }
}
