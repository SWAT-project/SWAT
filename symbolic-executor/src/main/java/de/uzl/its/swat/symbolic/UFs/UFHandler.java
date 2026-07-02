package de.uzl.its.swat.symbolic.UFs;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.thread.ThreadHandler;
import static java.lang.Thread.currentThread;

public class UFHandler {
    private EqualsIgnoreCaseUF equalsIgnoreCaseUF;
    private ToLowerCaseUF toLowerCaseUF;
    private SinCosUF sinCosUF;
    private PureFunctionUF pureFunctionUF;

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

    public SinCosUF getSinCosUF() throws NoThreadContextException {
        if (sinCosUF == null) {
            sinCosUF = new SinCosUF(ThreadHandler.getSolverContext(currentThread().getId()));
        }
        return sinCosUF;
    }

    /** Registry of generic uninterpreted functions for whitelisted pure JDK methods. */
    public PureFunctionUF getPureFunctionUF() throws NoThreadContextException {
        if (pureFunctionUF == null) {
            pureFunctionUF = new PureFunctionUF(ThreadHandler.getSolverContext(currentThread().getId()));
        }
        return pureFunctionUF;
    }
}
