package de.uzl.its.swat.symbolic.value.reference;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.reference.array.ObjectArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BoxedValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The shadow values a JDK container (a {@code java.util} collection or map, or a view, iterator or
 * entry derived from one) has been given through modelled calls. A container is not instrumented,
 * so its fields stay empty in the shadow; the shadow of what it holds is kept here instead.
 *
 * <p>The record only grows: removals are not tracked, so it over-approximates the contents. That
 * is enough for its two uses:
 *
 * <ul>
 *   <li>{@link #isSymbolic} makes a container that may hold a symbolic value count as symbolic,
 *       so an unmodelled call that gets it still records context loss;
 *   <li>{@link #lookup} gives back the shadow of an element a modelled read returned, found by the
 *       address of the concrete object.
 * </ul>
 *
 * <p>Contents can have sources: the contents of another container (a copy, a view) or an object
 * array (the array behind {@code Arrays.asList}). Their values count as values of this container.
 */
public class ContainerContents {

    /** What the container is, which decides the calls that are modelled on it. */
    public enum Kind {
        /** A list, queue or deque: elements are only stored and returned, never compared. */
        LIST,
        /** A hash map: the keys are hashed and compared, the values are only stored and returned. */
        MAP,
        /** A view, iterator, enumeration or map entry, or an immutable container: only read. */
        VIEW
    }

    /** Marks an address under which different shadows were stored. */
    private static final Value<?, ?> AMBIGUOUS = new PlaceHolder(false);

    public final Kind kind;
    /** Whether the elements of this view are map entries (an entry set and its iterators). */
    public final boolean yieldsEntries;

    /** Mutable shadows (objects, arrays, containers), whose symbolic state can change later. */
    private final List<Value<?, ?>> mutables = new ArrayList<>();
    /** Whether a value-typed (String, boxed) element was symbolic when it was stored. */
    private boolean symbolicImmutable = false;
    /** Map keys; a modelled store only accepts keys that are not symbolic. */
    private final List<Value<?, ?>> keys = new ArrayList<>();

    private final Map<Integer, Value<?, ?>> byAddress = new HashMap<>();
    private final List<Value<?, ?>> addressUnknown = new ArrayList<>();
    private final List<Object> sources = new ArrayList<>();

    public ContainerContents(Kind kind) {
        this(kind, false);
    }

    public ContainerContents(Kind kind, boolean yieldsEntries) {
        this.kind = kind;
        this.yieldsEntries = yieldsEntries;
    }

    /** A view of {@code source}: a read-only container that holds what the source holds. */
    public static ContainerContents viewOf(ContainerContents source, boolean yieldsEntries) {
        ContainerContents view = new ContainerContents(Kind.VIEW, yieldsEntries);
        view.addSource(source);
        return view;
    }

    public void addValue(Value<?, ?> value) throws NotImplementedException {
        if (value == null) {
            return;
        }
        if (isValueTyped(value)) {
            symbolicImmutable = symbolicImmutable || value.isSymbolic();
        } else if (value instanceof ObjectValue<?, ?> ov && !ov.isNullValue()) {
            mutables.add(value);
        } else if (!(value instanceof ObjectValue<?, ?>)) {
            symbolicImmutable = symbolicImmutable || value.isSymbolic();
        }
        index(value);
    }

    public void addKey(Value<?, ?> key) throws NotImplementedException {
        if (key == null) {
            return;
        }
        keys.add(key);
        index(key);
    }

    /** Adds the contents of another container, or the elements of an object array. */
    public void addSource(Object source) {
        if (source != this && (source instanceof ContainerContents || source instanceof ObjectArrayValue)) {
            for (Object s : sources) {
                if (s == source) return;
            }
            sources.add(source);
        }
    }

    /** Whether a symbolic value may be stored in the container (keys included). */
    public boolean isSymbolic(Set<Value<?, ?>> visited) throws NotImplementedException {
        return isSymbolic(visited, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private boolean isSymbolic(Set<Value<?, ?>> visited, Set<Object> seen) throws NotImplementedException {
        if (!seen.add(this)) {
            return false;
        }
        if (symbolicImmutable) {
            return true;
        }
        for (Value<?, ?> v : mutables) {
            if (v.isSymbolic(visited)) return true;
        }
        if (keysSymbolic(visited)) {
            return true;
        }
        for (Object s : sources) {
            if (s instanceof ContainerContents c && c.isSymbolic(visited, seen)) return true;
            if (s instanceof ObjectArrayValue a && seen.add(a) && a.isSymbolic(visited)) return true;
        }
        return false;
    }

    /** Whether a map key stored here (not in the sources) has become symbolic. */
    public boolean keysSymbolic(Set<Value<?, ?>> visited) throws NotImplementedException {
        for (Value<?, ?> k : keys) {
            if (k.isSymbolic(visited)) return true;
        }
        return false;
    }

    /**
     * The shadow of the value with this address that was stored in the container or one of its
     * sources, or {@code null} if there is none or there are several different ones.
     */
    public Value<?, ?> lookup(int address) {
        Value<?, ?> found = lookup(address, Collections.newSetFromMap(new IdentityHashMap<>()));
        return found == AMBIGUOUS ? null : found;
    }

    private Value<?, ?> lookup(int address, Set<Object> seen) {
        if (!seen.add(this)) {
            return null;
        }
        resolvePendingAddresses();
        Value<?, ?> found = byAddress.get(address);
        for (Object s : sources) {
            Value<?, ?> candidate = null;
            if (s instanceof ContainerContents c) {
                candidate = c.lookup(address, seen);
            } else if (s instanceof ObjectArrayValue a && seen.add(a)) {
                candidate = lookupInArray(a, address);
            }
            found = merge(found, candidate);
        }
        return found;
    }

    private static Value<?, ?> lookupInArray(ObjectArrayValue array, int address) {
        Value<?, ?> found = null;
        Value<?, ?>[] elements = array.getFields();
        if (elements == null) {
            return null;
        }
        for (Value<?, ?> e : elements) {
            if (e instanceof ObjectValue<?, ?> ov && ov.getAddress() == address) {
                found = merge(found, e);
            }
        }
        return found;
    }

    private static Value<?, ?> merge(Value<?, ?> a, Value<?, ?> b) {
        if (a == null) return b;
        if (b == null || a == b) return a;
        return AMBIGUOUS;
    }

    private void index(Value<?, ?> value) {
        if (!(value instanceof ObjectValue<?, ?> ov) || ov.isNullValue()) {
            return;
        }
        if (ov.getAddress() == ObjectValue.ADDRESS_UNKNOWN) {
            addressUnknown.add(value);
        } else {
            byAddress.merge(ov.getAddress(), value, ContainerContents::merge);
        }
    }

    /** Values stored before their address was known are indexed once it is. */
    private void resolvePendingAddresses() {
        if (addressUnknown.isEmpty()) {
            return;
        }
        List<Value<?, ?>> pending = new ArrayList<>(addressUnknown);
        addressUnknown.clear();
        for (Value<?, ?> v : pending) {
            index(v);
        }
    }

    /** Strings and boxed values: their shadow is fixed once created. */
    private static boolean isValueTyped(Value<?, ?> value) {
        return value instanceof StringValue || value instanceof BoxedValue<?>;
    }
}
