package de.uzl.its.swat.common;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.logger.SystemLogger;

public class ErrorHandler {

    private final Config config;
    private final SystemLogger systemLogger;

    public ErrorHandler() {
        this.config = Config.instance();
        systemLogger = new SystemLogger();
    }

    public void handleException(Exception e) {
        handleException("An error occurred, but was swallowed", e);
    }

    public void handleException(String msg, Throwable t) {
        if (config.exitOnError()) {
            throw new RuntimeException(t); // Rethrow as unchecked exception
        } else {
            systemLogger.logError(msg, t);
        }
    }
}
