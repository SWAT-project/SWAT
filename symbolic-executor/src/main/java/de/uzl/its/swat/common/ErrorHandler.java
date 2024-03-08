package de.uzl.its.swat.common;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.request.Request;
import org.slf4j.LoggerFactory;

/**
 * ErrorHandler is responsible for handling exceptions throughout the application. It decides
 * whether to log the error or terminate the execution based on the application configuration.
 */
public class ErrorHandler {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

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
     * Handles an exception with a custom message. Depending on the application configuration, it
     * either logs the error or terminates the execution by throwing a RuntimeException.
     *
     * @param msg the custom message to log along with the exception
     * @param t the throwable to handle
     */
    public void handleException(String msg, Throwable t) {
        if (config.exitOnError()) {
            // Consider a more specific custom exception or handling strategy
            throw new CustomRuntimeException("Critical error occurred", t);
        } else {
            logger.error(msg + " " + t);
        }
    }
}

/** ILogger interface for logging errors. This allows using different logging implementations. */
interface ILogger {
    void logError(String message, Throwable t);
}

/** CustomRuntimeException to encapsulate exceptions that lead to application termination. */
class CustomRuntimeException extends RuntimeException {
    public CustomRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
