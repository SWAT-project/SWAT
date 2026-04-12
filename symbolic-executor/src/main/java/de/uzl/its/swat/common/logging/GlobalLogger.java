package de.uzl.its.swat.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.config.Config;

// ToDo: Not thread-safe yet
public class GlobalLogger {

    private static Logger solutionLogger;
    private static Logger symbolicExecutionLogger;
    private static Logger errorLogger;


    public static Logger getSymbolicExecutionLogger() {

        if (symbolicExecutionLogger == null) {

                boolean logToConsole = Config.instance().isLogToConsole();
                Level logLevel =
                        Config.instance().isDisableSymbolicExecutionLogging()
                                ? Level.OFF
                                : Config.instance().getLoggingLevel();

                symbolicExecutionLogger =
                        LoggerUtils.createGlobalLogger(
                                "symbolic-execution",
                                null,
                                logToConsole,
                                logLevel);

            LoggerUtils.addErrorOnlyFileAppender(symbolicExecutionLogger, "errors.log", null);
        }

        return symbolicExecutionLogger;
    }    

    public static Logger getErrorLogger() {

        if (errorLogger == null) {

                errorLogger =
                        LoggerUtils.createGlobalLogger(
                                "error",
                                null,
                                true,
                                Level.ERROR);

            LoggerUtils.addErrorOnlyFileAppender(errorLogger, "errors.log", null);
        }

        return errorLogger;
    }

    public static Logger getSolutionLogger() {


        if (solutionLogger == null) {

            boolean logToConsole = Config.instance().isLogToConsole();
            solutionLogger =
                    LoggerUtils.createGlobalLogger(
                            "solution",
                            null,
                            logToConsole,
                            Config.instance().getLoggingLevel());
            
            LoggerUtils.addErrorOnlyFileAppender(solutionLogger, "errors.log", null);
        }

        return solutionLogger;
    }
}
