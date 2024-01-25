package de.uzl.its.swat.logger;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.interpreters.SymbolicInterpreter;
import de.uzl.its.swat.logger.inst.Instruction;
import de.uzl.its.swat.thread.ThreadHandler;

/**
 * ToDo Flo: Is DirectConcolicExecution describing what this class is doing? IMHO: It is just
 * visting a thus instrumenting instructions?
 */
public class DirectConcolicExecution extends AbstractLogger {
    Config config = Config.instance();

    public DirectConcolicExecution() {}

    @Override
    protected void log(Instruction nextInstruction) {
        long id = Thread.currentThread().getId();
        Instruction currentInstruction = ThreadHandler.getCurrentInstruction(id);

        SymbolicInterpreter currIntp = ThreadHandler.getConcolicInterpreter(id);

        if (currentInstruction == null) {
            ThreadHandler.setCurrentInstruction(id, nextInstruction);
        } else {
            ThreadHandler.setNextInstruction(id, nextInstruction);
            currIntp.setNext(ThreadHandler.getNextInstruction(id));
            try {
                currentInstruction.visit(currIntp);
            } catch (Exception e) {
                new ErrorHandler().handleException("Error visiting Instruction!", e);
            }
            ThreadHandler.setCurrentInstruction(id, ThreadHandler.getNextInstruction(id));
        }
    }
}
