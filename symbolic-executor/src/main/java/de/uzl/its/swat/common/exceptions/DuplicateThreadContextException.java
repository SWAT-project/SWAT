package de.uzl.its.swat.common.exceptions;

public class DuplicateThreadContextException extends Exception {
    public DuplicateThreadContextException(long threadId) {
        super("Duplicate context available for thread " + threadId);
    }
}
