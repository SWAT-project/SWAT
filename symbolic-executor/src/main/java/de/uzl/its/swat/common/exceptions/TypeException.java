package de.uzl.its.swat.common.exceptions;

import de.uzl.its.swat.instrument.DataType;
import de.uzl.its.swat.symbolic.value.ValueType;

public class TypeException extends Exception {
    public TypeException(char type) {
        super("Unexpected type: " + type);
    }
    public TypeException(DataType type) {
        super("Unexpected type: " + type);
    }

    public TypeException(ValueType type) {
        super("Unexpected type: " + type);
    }
}
