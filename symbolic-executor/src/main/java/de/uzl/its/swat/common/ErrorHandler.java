package de.uzl.its.swat.common;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.FileAppender;
import de.uzl.its.swat.common.exceptions.SWATException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.ErrorRecord;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.thread.ThreadHandler;

import java.util.Arrays;

import static java.lang.Thread.currentThread;
/**
 * ErrorHandler is responsible for handling exceptions throughout the application. It decides
 * whether to log the error or terminate the execution based on the application configuration.
 */
public class ErrorHandler {
    private static final Logger logger = GlobalLogger.getErrorLogger();

    private final Config config;

    /** Constructs an ErrorHandler with a specified Config and ILogger implementation. */
    public ErrorHandler() {
        this.config = Config.instance();
    }

    /**
     * Handles an exception by logging it with a default message. The action taken depends on the
     * application configuration.
     *
     * @param e the exception to handle
     */
    public void handleException(Exception e) {
        handleException("An error occurred, but was swallowed", e);
    }

    /**
     * Raises a SWAT exception.
     * @param msg the error message
     */
    public void raiseException(String msg) {
        handleException(new SWATException(msg));
    }

    /**
     * Handles an exception with a custom message. Depending on the application configuration, it
     * either logs the error or terminates the execution by throwing a RuntimeException.
     *
     * @param msg the custom message to log along with the exception
     * @param t the throwable to handle
     */
    public void handleException(String msg, Throwable t) {
        long instCnt = -1;
        try {
            instCnt = ThreadHandler.getInstructionCount(currentThread().getId());
        } catch (Exception e) {
            logger.error("Error while getting instruction count");
        }
        msg = "[SWAT Exception]: " + msg + " (at instruction: " + instCnt + ")";
        logException(msg, t);
        logger.error(msg);
        logger.error("Preparing to halt execution due to error...");
        try {
            if (ThreadHandler.hasThreadContext(currentThread().getId())) {
                ThreadHandler.logStats(currentThread().getId());
            }
            ThreadHandler.logStats(-1);
        } catch (Exception e) {
            logger.error("Error while logging stats");
        }
        logger.error("Halting...");
        if (!config.isLogShadowStateToConsole()) {
            logger.info("Flushing loggers...");
            try {
                long id = Thread.currentThread().getId();
                Logger shadowStateLogger = ThreadHandler.getShadowStateLogger(id);
                ((FileAppender<?>)((AsyncAppender) shadowStateLogger
                        .getAppender("ASYNC_FILE"))
                        .getAppender("FILE")).getOutputStream().flush();
            } catch (Exception e) {
                logger.error("Error while flushing loggers", e);
            }
        }
        Runtime.getRuntime().halt(-1);

    }

    public void logException(String msg, Throwable t){
        logger.error(msg, t);
        long threadId = currentThread().getId();
        String executionStage;
        if(ThreadHandler.hasThreadContext(threadId)){
            try {
                executionStage = ThreadHandler.getCurrentInstruction(threadId).toString();
            } catch (Exception e) {
                logger.error("Error while getting current instruction", e);
                executionStage = "Unknown";
            }
        }  else {
            executionStage = "Main Thread";
            threadId = -1;
        }

        // Record the error
        ErrorRecord errorRecord = new ErrorRecord(msg, t.getClass().getSimpleName(),
                Arrays.toString(t.getStackTrace()), t.getMessage(), executionStage);
        try{
            ThreadHandler.recordException(threadId, errorRecord);
        } catch (Exception e) {
            logger.error("Error while recording exception", e);
        }
    }

}
