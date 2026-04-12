package de.uzl.its.swat.common.exceptions;

/** Exception class used when shadow values cannot be converted to the requested type. */
public class ValueConversionException extends Exception {
    public ValueConversionException(String message) {
        super(message);
    }
}
