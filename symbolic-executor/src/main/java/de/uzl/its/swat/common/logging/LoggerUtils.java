package de.uzl.its.swat.common.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.config.Config;
import org.slf4j.LoggerFactory;

public class LoggerUtils {
    private static final String fileAppenderName = "FILE";
    private static final String consoleAppenderName = "CONSOLE";
    private static final String errorFileAppenderName = "ERROR_FILE";

    public static Logger createAsyncFileLogger(String baseName,
                                               String dir,
                                               Level level) {

        baseName = sanitizeEndpointName(baseName);
        String uniqueSuffix = getUniqueSuffix(baseName, dir);
        baseName = baseName + uniqueSuffix;
        Logger logger = (Logger) LoggerFactory.getLogger("shadow-state-logger-" + baseName);

        FileAppender<ILoggingEvent> fileAppender = createFileAppender(getShadowLogFile(baseName, dir));

        if (logger.getAppender("ASYNC_FILE") == null) {

            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            AsyncAppender async = new AsyncAppender();
            async.setContext(lc);
            async.setName("ASYNC_FILE");
            async.setQueueSize(50000);           // buffer up to 500 events
            async.setDiscardingThreshold(0);   // when full, block the caller
            async.addAppender(fileAppender);
            async.start();

            logger.addAppender(async);
        }

        logger.setLevel(level != null ? level : Level.INFO);
        logger.setAdditive(false);
        return logger;
    }


    public static Logger createGlobalLogger(
            String baseName, String dir, boolean logToConsole, Level level) {

        Logger logger = (Logger) LoggerFactory.getLogger(baseName + "-logger");

        if (logger.getAppender(fileAppenderName) == null) {
            FileAppender<ILoggingEvent> fileAppender = createFileAppender(getGlobalLogFile(baseName, dir));
            logger.addAppender(fileAppender);
        }
        if (logToConsole && logger.getAppender(consoleAppenderName) == null) {
            ConsoleAppender<ILoggingEvent> consoleAppender = createConsoleAppender();
            logger.addAppender(consoleAppender);
        }
        logger.setLevel(level != null ? level : Level.INFO);
        logger.setAdditive(false); /* set to true if root should log too */

        return logger;
    }

    private static FileAppender<ILoggingEvent> createFileAppender(String logFile) {

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setName(fileAppenderName);
        fileAppender.setImmediateFlush(false); // don't flush after every write to decrease I/O overhead

        fileAppender.setFile(logFile);
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();

        return fileAppender;
    }

    private static ConsoleAppender<ILoggingEvent> createConsoleAppender() {

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("%date %level [%thread] [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<ILoggingEvent>();
        consoleAppender.setName(consoleAppenderName);

        consoleAppender.setEncoder(ple);
        consoleAppender.setContext(lc);
        consoleAppender.start();

        return consoleAppender;
    }

    private static FileAppender<ILoggingEvent> createErrorOnlyFileAppender(String fileName, String dir) {
        
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();

        FileAppender<ILoggingEvent> errorFileAppender = new FileAppender<ILoggingEvent>();
        errorFileAppender.setName(errorFileAppenderName);
        errorFileAppender.setImmediateFlush(true);
        
        String loggingDir;
        if (dir == null) {
            loggingDir = Config.instance().getLoggingDirectory();
        } else {
            loggingDir = dir;
        }
        
        errorFileAppender.setFile(loggingDir + "/" + fileName);
        errorFileAppender.setEncoder(ple);
        errorFileAppender.setContext(lc);

        ThresholdFilter errorFilter = new ThresholdFilter();
        errorFilter.setLevel("ERROR");
        errorFilter.setContext(lc);
        errorFilter.start();
        errorFileAppender.addFilter(errorFilter);

        errorFileAppender.start();
        
        return errorFileAppender;
    }

    public static void addErrorOnlyFileAppender(Logger logger, String errorFileName, String dir) {
        if (logger.getAppender(errorFileAppenderName) == null) {
            FileAppender<ILoggingEvent> errorFileAppender = createErrorOnlyFileAppender(errorFileName, dir);
            logger.addAppender(errorFileAppender);
        }
    }

    private static String getShadowLogFile(String baseName, String dir) {
        String loggingDir = (dir == null) ? Config.instance().getLoggingDirectory() : dir;
        return loggingDir + "/shadow-state/" + baseName + ".log";
    }


    private static String getGlobalLogFile(String baseName, String dir) {
        String loggingDir = (dir == null) ? Config.instance().getLoggingDirectory() : dir;
        return loggingDir + "/global/" + baseName + ".log";
    }

    private static String getUniqueSuffix(String endpointName, String dir) {
        java.io.File originalFile = new java.io.File(getShadowLogFile(endpointName, dir));

        if (!originalFile.exists()) {
            return "";
        }

        SWATAssert.enforce(!endpointName.contains("."), "Endpoint name should not contain a dot: {}", endpointName);

        int counter = 0;
        String newFileName;
        java.io.File newFile;
        do {
            counter++;
            newFileName = endpointName + "-" + counter;
            newFile = new java.io.File(getShadowLogFile(newFileName, dir));
        } while (newFile.exists());

        return "-" + counter;
    }
    private static String sanitizeEndpointName(String endpointName) {
        int openParen = endpointName.indexOf('('); // The end of the Class name

        if (openParen == -1) {
            return endpointName;
        }

        String cname = endpointName.substring(0, openParen);
        String desc = endpointName.substring(openParen).replace('/', '_');
        return cname + desc ;
    }
}
