package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.array.AbstractArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.ArrayArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.ObjectArrayValue;
import java.lang.reflect.Array;
import org.objectweb.asm.Type;

public final class SystemInvocation {

    /** Longer copies are not modelled: each element adds a store to the array formula. */
    static final int MAX_ARRAYCOPY_LENGTH = 4096;

    private SystemInvocation() {}

    public static Value<?, ?> invokeStaticMethod(String name, Value<?, ?>[] args, Type[] desc)
            throws NotImplementedException {
        if (name.equals("arraycopy") && args.length == 5) {
            return invokeArraycopy(args);
        }
        return PlaceHolder.instance;
    }

    /**
     * {@code System.arraycopy(src, srcPos, dest, destPos, length)} with concrete positions and
     * length: the shadow elements are copied as the JVM copies the elements, as if through a
     * temporary array. The model runs after the concrete call returned, so the arguments were
     * valid.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Value<?, ?> invokeArraycopy(Value<?, ?>[] args) throws NotImplementedException {
        for (int i : new int[] {1, 3, 4}) {
            if (!(args[i] instanceof IntValue) || args[i].isSymbolic()) return PlaceHolder.instance;
        }
        int srcPos = ((IntValue) args[1]).concrete;
        int destPos = ((IntValue) args[3]).concrete;
        int length = ((IntValue) args[4]).concrete;
        if (length < 0 || length > MAX_ARRAYCOPY_LENGTH) {
            return PlaceHolder.instance;
        }
        Value<?, ?> src = args[0];
        Value<?, ?> dest = args[2];

        if (src instanceof ObjectArrayValue s && dest instanceof ObjectArrayValue d) {
            Value<?, ?>[] from = s.getFields();
            Value<?, ?>[] to = d.getFields();
            if (from == null || to == null || srcPos + length > from.length || destPos + length > to.length) {
                return PlaceHolder.instance;
            }
            System.arraycopy(from, srcPos, to, destPos, length);
            return VoidValue.instance;
        }

        if (!(src instanceof AbstractArrayValue s) || !(dest instanceof AbstractArrayValue d)
                || src.getClass() != dest.getClass() || src instanceof ArrayArrayValue<?>) {
            return PlaceHolder.instance;
        }
        if (s.concrete == null || d.concrete == null
                || srcPos + length > Array.getLength(s.concrete) || destPos + length > Array.getLength(d.concrete)) {
            return PlaceHolder.instance;
        }
        // The shadow of an array seen through GETVALUE keeps the real array as its concrete value,
        // which the call has already updated; its concrete values must not be written again.
        boolean destIsLive = System.identityHashCode(d.concrete) == d.getAddress();
        Object liveValues = null;
        if (destIsLive) {
            liveValues = Array.newInstance(d.concrete.getClass().getComponentType(), length);
            System.arraycopy(d.concrete, destPos, liveValues, 0, length);
        }
        Value<?, ?>[] elements = new Value[length];
        for (int k = 0; k < length; k++) {
            elements[k] = s.getElement(new IntValue(s.getContext(), srcPos + k));
        }
        for (int k = 0; k < length; k++) {
            d.storeElement(new IntValue(d.getContext(), destPos + k), elements[k]);
        }
        if (destIsLive) {
            System.arraycopy(liveValues, 0, d.concrete, destPos, length);
        }
        return VoidValue.instance;
    }
}
