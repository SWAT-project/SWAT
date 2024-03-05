package de.uzl.its.symbolic.value;

import de.uzl.its.symbolic.value.reference.ObjectValue;

public class VoidValue extends Value {
    public static final VoidValue instance = new VoidValue();

    public ObjectValue<?, ?> asObjectValue() {
        throw new RuntimeException("Cannot convert VoidValue to ObjectValue");
    }

    @Override
    public String toString() {
        return "Void ()";
    }
}
