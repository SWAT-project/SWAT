package de.uzl.its.swat.common.exceptions;

public class ThreadAlreadyEnabledException extends Exception {
    public ThreadAlreadyEnabledException(long threadId) {
        super("Thread already enabled: " + threadId);
    }
}
