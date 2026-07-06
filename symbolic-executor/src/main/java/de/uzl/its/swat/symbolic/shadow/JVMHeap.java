package de.uzl.its.swat.symbolic.shadow;

import com.google.common.collect.MapMaker;
import de.uzl.its.swat.symbolic.value.Value;

import java.util.Map;

/**
 * Canonical shadow registry: maps a concrete object reference to its shadow {@link Value}.
 *
 * <p>The key is the concrete object itself, compared by reference identity ({@code ==}), not by
 * {@code System.identityHashCode}. This is collision-free (distinct objects are distinct keys even
 * when their identity hash collides) and gives exactly one shadow per concrete object.
 *
 * <p>Keys are held weakly ({@link MapMaker#weakKeys()}). For plain objects and boxed primitives the
 * shadow value holds no strong reference to its concrete key, so an entry is evicted once the
 * concrete object becomes unreachable. NOTE: a {@code StringValue} stores its own concrete
 * {@code String}, which is also the key, so String-keyed entries are self-pinned and do not evict
 * until the thread's context is discarded. Reference keying still removes the identity-hash
 * collision/reuse hazard; a unique-id key (which would also evict Strings) is a possible future
 * improvement.
 */
public class JVMHeap {

    private final Map<Object, Value<?, ?>> objects;

    public JVMHeap() {
        // weakKeys() => identity (==) key comparison + weak references.
        objects = new MapMaker().weakKeys().makeMap();
    }

    public void put(Object ref, Value<?, ?> value) {
        if (ref == null) {
            return;
        }
        objects.put(ref, value);
    }

    public Value<?, ?> get(Object ref) {
        if (ref == null) {
            return null;
        }
        return objects.get(ref);
    }

    /**
     * Number of registered cells. With reference keying this is the number of distinct concrete
     * objects currently held (collision-free).
     *
     * @return the number of entries currently held.
     */
    public int size() {
        return objects.size();
    }
}
