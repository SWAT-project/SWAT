package de.uzl.its.swat.symbolic.shadow;

import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;

import java.util.HashMap;
import java.util.Map;

public class JVMHeap {

    private final Map<Integer, Value<?, ?>> objects;

    public JVMHeap() {
        objects = new HashMap<>();
    }

    public void put(int hashCode, Value<?, ?> value) {
        objects.put(hashCode, value);
    }

    public Value<?, ?> get(int hashCode) {
        return objects.get(hashCode);
    }
}
