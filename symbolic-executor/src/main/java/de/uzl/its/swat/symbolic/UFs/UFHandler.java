package de.uzl.its.swat.symbolic.UFs;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.thread.ThreadHandler;
import static java.lang.Thread.currentThread;

public class UFHandler {
    private EqualsIgnoreCaseUF equalsIgnoreCaseUF;
    private CharToLowerCaseUF charToLowerCaseUF;
    private StringToLowerCaseUF stringToLowerCaseUF;
    private SinCosUF sinCosUF;

    public EqualsIgnoreCaseUF getEqualsIgnoreCaseUF() throws NoThreadContextException {
        if (equalsIgnoreCaseUF == null) {
            equalsIgnoreCaseUF = new EqualsIgnoreCaseUF(ThreadHandler.getSolverContext(currentThread().getId()));
        }
        return equalsIgnoreCaseUF;
    }

    public CharToLowerCaseUF getCharToLowerCaseUF() throws NoThreadContextException {
        if (charToLowerCaseUF == null) {
            charToLowerCaseUF = new CharToLowerCaseUF(ThreadHandler.getSolverContext(currentThread().getId()));
        }
        return charToLowerCaseUF;
    }

    public StringToLowerCaseUF getStringToLowerCaseUF() throws NoThreadContextException {
        if (stringToLowerCaseUF == null) {
            stringToLowerCaseUF = new StringToLowerCaseUF(ThreadHandler.getSolverContext(currentThread().getId()));
        }
        return stringToLowerCaseUF;
    }

    public SinCosUF getSinCosUF() throws NoThreadContextException {
        if (sinCosUF == null) {
            sinCosUF = new SinCosUF(ThreadHandler.getSolverContext(currentThread().getId()));
        }
        return sinCosUF;
    }
}
