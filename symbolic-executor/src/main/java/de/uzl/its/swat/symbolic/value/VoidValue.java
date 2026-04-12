package de.uzl.its.swat.symbolic.value;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;

public class VoidValue extends Value {
    public static final VoidValue instance = new VoidValue();
    public boolean isSymbolic = false;

    public VoidValue(boolean isSymbolic) {
        super();
        this.isSymbolic = isSymbolic;
    }
    public VoidValue() {
        super();
        isSymbolic = false;
    }

    public boolean isSymbolic() {
        return isSymbolic;
    }

    public ObjectValue<?, ?> asObjectValue() throws ValueConversionException {
        throw new ValueConversionException("Cannot convert void to object");
    }

    @Override
    public String toString() {
        return "Void ()";
    }
}
