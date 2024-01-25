package de.uzl.its.value.reference.lang;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.SolverContext;
import spock.lang.Specification;

public class StringValueTest extends Specification {

    def context =
            SolverContextFactory.createSolverContext(
                    Configuration.defaultConfiguration(),
                    BasicLogManager.create(Configuration.defaultConfiguration()),
                    ShutdownManager.create().getNotifier(),
                    SolverContextFactory.Solvers.Z3);
    def prover = context.newProverEnvironment(
            SolverContext.ProverOptions.GENERATE_MODELS)
    def bmgr = context.getFormulaManager().getBooleanFormulaManager()
    def smgr = context.getFormulaManager().getStringFormulaManager()
}

