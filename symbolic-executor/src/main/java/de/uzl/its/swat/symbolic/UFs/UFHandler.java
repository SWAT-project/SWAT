package de.uzl.its.swat.symbolic.UFs;

import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.thread.ThreadHandler;
import static java.lang.Thread.currentThread;

public class UFHandler {
    private SinCosUF sinCosUF;

    public SinCosUF getSinCosUF() throws NoThreadContextException {
        if (sinCosUF == null) {
            sinCosUF = new SinCosUF(ThreadHandler.getSolverContext(currentThread().getId()));
        }
        return sinCosUF;
    }
}
