package de.uzl.its.swat.common.exceptions;

/**
 * A runtime exception used by SWAT to signal fatal errors
 */
public class SWATException extends Exception {
    public SWATException(String message) {
        super(message);
    }

    public SWATException(String message, Throwable cause) {
        super(message, cause);
    }
}