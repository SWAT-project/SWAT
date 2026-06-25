package de.uzl.its.swat.symbolic.shadow;

import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;

import java.util.Collection;
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
    /**
     * Number of registered cells. On the legacy identity-hash-keyed heap this also equals the
     * number of distinct keys, so colliding identities are undercounted (a known defect).
     *
     * @return the number of entries currently held.
     */
    public int size() {
        return objects.size();
    }

    /**
     * All registered shadow values, for "one wrapper per identity" inspection.
     *
     * @return the registered values.
     */
    public Collection<Value<?, ?>> values() {
        return objects.values();
    }
}
