package de.uzl.its.swat.symbolic.UFs;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.thread.ThreadHandler;
import org.sosy_lab.java_smt.api.SolverContext;

import static java.lang.Thread.currentThread;

public class UFHandler {
    private EqualsIgnoreCaseUF equalsIgnoreCaseUF;
    private ToLowerCaseUF toLowerCaseUF;


    public EqualsIgnoreCaseUF getEqualsIgnoreCaseUF() throws NoThreadContextException {
        if (equalsIgnoreCaseUF == null) {
            equalsIgnoreCaseUF = new EqualsIgnoreCaseUF(ThreadHandler.getSolverContext(currentThread().getId()));
        }
        return equalsIgnoreCaseUF;
    }

    public ToLowerCaseUF getToLowerCaseUF() throws NoThreadContextException {
        if (toLowerCaseUF == null) {
            toLowerCaseUF = new ToLowerCaseUF(ThreadHandler.getSolverContext(currentThread().getId()));
        }
        return toLowerCaseUF;
    }
}
