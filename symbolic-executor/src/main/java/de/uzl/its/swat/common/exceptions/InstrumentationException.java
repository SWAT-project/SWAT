package de.uzl.its.swat.common.exceptions;

/**
 * An instrumentation time exception used by SWAT to signal fatal errors during target instrumentation.
 */
public class InstrumentationException extends RuntimeException {
    public InstrumentationException(String message) {
        super("Error during instrumentation: " + message);
    }
    public InstrumentationException(Throwable cause) {
        super("Error during instrumentation", cause);
    }
    public InstrumentationException(String cname, Throwable cause) {
        super("Error during instrumentation of class: " + cname, cause);
    }
}