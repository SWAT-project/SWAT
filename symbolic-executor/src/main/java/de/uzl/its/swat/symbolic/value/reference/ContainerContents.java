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
        /** A hash set, modelled only while its exact elements are known ({@link HashEntries}). */
        SET,
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
    /** Mutable map keys; a modelled store only accepts keys that are not symbolic. */
    private final List<Value<?, ?>> keys = new ArrayList<>();

    private final Map<Integer, Value<?, ?>> byAddress = new HashMap<>();
    private final List<Value<?, ?>> addressUnknown = new ArrayList<>();
    private final List<Object> sources = new ArrayList<>();

    /**
     * For a list built only by appending since its modelled construction: the stored shadows in
     * list order, so that {@code get(i)} and {@code toArray} can take the shadow at the index. The
     * address alone is ambiguous when one object is stored twice with different shadows (e.g. the
     * empty string, which StringBuilder.toString() returns as one shared object). Null once
     * anything else changed the list (see CollectionInvocation).
     */
    private List<Value<?, ?>> ordered;
    /** Whether {@link #ordered} is exact: no call that is not modelled got the list since. */
    private boolean orderExact = true;

    /**
     * For a hash map or set: its exact entries, or null if they are not known (the container was
     * not seen created, or a call on it was not modelled exactly, see CollectionInvocation).
     */
    private HashEntries hash;
    /**
     * Whether a symbolic String or boxed key was stored (maps), or a symbolic element (sets). Such
     * a container is modelled only through its exact entries; once those are lost, every call on
     * it is a context loss.
     */
    private boolean symbolicKeys = false;
    /**
     * Whether the container was filled from a hash container whose iteration order was unknown
     * then (a list copied from a HashSet with symbolic elements): the order of its own elements
     * is unknown for good.
     */
    private boolean hashOrdered = false;
    /** For a view: the map whose key set it is ({@code Map.keySet()}), see {@link #keySetOf()}. */
    private ContainerContents keySetOf;

    public ContainerContents(Kind kind) {
        this(kind, false);
    }

    public ContainerContents(Kind kind, boolean yieldsEntries) {
        this.kind = kind;
        this.yieldsEntries = yieldsEntries;
        this.ordered = kind == Kind.LIST ? new ArrayList<>() : null;
    }

    /** The stored shadows in list order, or null if the order is not known (see {@link #ordered}). */
    public List<Value<?, ?>> ordered() {
        return ordered;
    }

    /** Records a shadow appended at the end of the list, if the order is still known. */
    public void appendOrdered(Value<?, ?> value) {
        if (ordered != null) {
            ordered.add(value);
        }
    }

    /** The list was emptied: its order is known again (nothing). */
    public void clearOrdered() {
        if (kind == Kind.LIST) {
            ordered = new ArrayList<>();
            orderExact = true;
        }
    }

    /**
     * The elements of the list in order, if they are known exactly: as {@link #ordered}, but null
     * also once a call that is not modelled may have changed the list.
     */
    public List<Value<?, ?>> exactOrdered() {
        return orderExact ? ordered : null;
    }

    /** The list changed in a way not tracked in order: positions are unknown from now on. */
    public void forgetOrder() {
        ordered = null;
    }

    /** The exact entries of a hash container, or null if they are not known. */
    public HashEntries hash() {
        return hash;
    }

    public void setHash(HashEntries hash) {
        this.hash = hash;
    }

    /** Whether a symbolic key or set element was ever stored in this container itself. */
    public boolean hasSymbolicKeys() {
        return symbolicKeys;
    }

    /** Records an element added to a hash set: a value of the container that is compared like a key. */
    public void addElement(Value<?, ?> element) throws NotImplementedException {
        if (element == null) {
            return;
        }
        addValue(element);
        if (element.isSymbolic()) {
            symbolicKeys = true;
        }
    }

    /**
     * Gives up the exact entries of this container, and of the containers a view shows (a call
     * that is not modelled may have changed them through the view).
     *
     * @return whether one of them holds (or held) a symbolic key: then nothing constrains what the
     *     container holds any more, which is a context loss
     */
    public boolean forgetHash() {
        return forgetHash(Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private boolean forgetHash(Set<Object> seen) {
        if (!seen.add(this)) {
            return false;
        }
        boolean symbolic = hash != null && symbolicKeys;
        hash = null;
        orderExact = false;
        if (kind == Kind.VIEW) {
            for (Object s : sources) {
                if (s instanceof ContainerContents c) symbolic |= c.forgetHash(seen);
            }
        }
        return symbolic;
    }

    /**
     * Whether the order in which this container, or one it shows or was copied from, iterates may
     * depend on a symbolic key (see {@link HashEntries#orderUnknown}), or its exact entries were
     * lost while it held one.
     */
    public boolean hashOrderUnknown() throws NotImplementedException {
        return hashOrderUnknown(Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private boolean hashOrderUnknown(Set<Object> seen) throws NotImplementedException {
        if (!seen.add(this)) {
            return false;
        }
        if (hashOrdered || (symbolicKeys && (hash == null || hash.orderUnknown()))) {
            return true;
        }
        for (Object s : sources) {
            if (s instanceof ContainerContents c && c.hashOrderUnknown(seen)) return true;
        }
        return false;
    }

    /**
     * For the contents of a view: the map it is the key set of, if it shows nothing else.
     */
    public ContainerContents keySetOf() {
        return keySetOf != null && sources.size() == 1 && sources.get(0) == keySetOf ? keySetOf : null;
    }

    /** Marks a view of {@code map} (its only source) as the map's key set. */
    public void setKeySetOf(ContainerContents map) {
        keySetOf = map;
    }

    /** Records that the elements were copied in an iteration order that is unknown (see {@link #hashOrdered}). */
    public void markHashOrdered() {
        hashOrdered = true;
    }

    /** Whether a container among the sources (transitively) ever stored a symbolic key. */
    private boolean sourceHasSymbolicKeys(Set<Object> seen) {
        if (!seen.add(this)) {
            return false;
        }
        for (Object s : sources) {
            if (s instanceof ContainerContents c && (c.symbolicKeys || c.sourceHasSymbolicKeys(seen))) return true;
        }
        return false;
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
        // A String or boxed key is concrete or symbolic for good; only mutable keys are rechecked.
        // A symbolic one is stored only by the exact model of a hash map (HashEntries).
        if (!isValueTyped(key)) {
            keys.add(key);
        } else if (key.isSymbolic()) {
            symbolicKeys = true;
            symbolicImmutable = true;
        }
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

    /**
     * Whether the map may hold a symbolic key that its exact entries do not account for: a mutable
     * key stored here that has become symbolic, a symbolic String or boxed key when the exact
     * entries are lost, or one in a map this one was copied from as a source.
     */
    public boolean keysSymbolic(Set<Value<?, ?>> visited) throws NotImplementedException {
        for (Value<?, ?> k : keys) {
            if (k.isSymbolic(visited)) return true;
        }
        if (symbolicKeys && hash == null) {
            return true;
        }
        return kind == Kind.MAP && sourceHasSymbolicKeys(Collections.newSetFromMap(new IdentityHashMap<>()));
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
