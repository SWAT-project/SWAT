package de.uzl.its.swat.common.exceptions;

public class ThreadAlreadyDisabledException extends Exception {
    public ThreadAlreadyDisabledException(long threadId) {
        super("Thread already disabled: " + threadId);
    }
}
