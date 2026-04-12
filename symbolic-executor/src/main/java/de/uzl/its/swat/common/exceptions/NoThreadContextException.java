package de.uzl.its.swat.common.exceptions;

public class NoThreadContextException extends Exception {
    public NoThreadContextException(long threadId) {
        super("No context available for thread " + threadId);
    }
}
