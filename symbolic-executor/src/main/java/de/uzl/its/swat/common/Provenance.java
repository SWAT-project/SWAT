package de.uzl.its.swat.common;

import com.google.common.collect.MapMaker;

import java.util.Map;

/**
 * Provenance tracking for reference equality of de-interned value types.
 *
 * <p>De-interning gives every produced value type a fresh identity so the reference-keyed shadow
 * heap stays sound. That fresh identity diverges from the real JVM identity, which would break
 * reference equality ({@code ==}). To model {@code ==} exactly, each de-interned copy records the
 * genuine original ("canonical") object it was made from, and {@code UtilInstrumented.refEquals}
 * compares {@link #root}s instead of the de-interned objects: two copies of the same interned literal
 * (or the same cached box, or the same returned object) share one canonical, so they compare equal,
 * exactly as the un-transformed program would.
 *
 * <p>The map is identity-keyed and weak-keyed (an entry lives only while its de-interned copy is
 * reachable). Values are held strongly on purpose: a copy's canonical must stay alive and stable for
 * as long as the copy can be {@code ==}-compared, otherwise {@link #root} could strand a live alias and
 * flip a genuine {@code ==} to false. Entries whose copy has been collected are reclaimed as the map
 * is subsequently written (guava weak-key maps clear stale entries on later writes, not eagerly under
 * GC); because the de-intern sites that write this map are hot, the retained set stays bounded during
 * an active run. Same primitive and reclamation behavior as the shadow heap ({@code JVMHeap}).
 *
 * <p>Chains are collapsed at insertion ({@link #record} stores the fully-resolved root), so {@link
 * #root} is always a single lookup and never depends on an intermediate link surviving GC.
 *
 * <p>This class is excluded from instrumentation and listed in {@code IGNORED_INVOCATIONS}, so the
 * symbolic executor never models or context-loss-flags its calls; {@link #root} returns a concrete
 * object whose only use is the (concrete) reference comparison in {@code refEquals}.
 */
public final class Provenance {

    private Provenance() {}

    /** de-interned copy (weak, identity key) -> its canonical/original (strong value). */
    private static final Map<Object, Object> ROOTS = new MapMaker().weakKeys().makeMap();

    /**
     * Record that {@code copy} (a freshly de-interned object) originates from {@code canonical} (the
     * genuine pre-de-intern object). Stores the fully-resolved root of {@code canonical} so chains stay
     * depth-1. No-op for nulls or a self-mapping.
     */
    public static void record(Object copy, Object canonical) {
        if (copy == null || canonical == null || copy == canonical) {
            return;
        }
        ROOTS.put(copy, root(canonical));
    }

    /**
     * The original identity {@code x} stands for: the recorded canonical, or {@code x} itself when
     * {@code x} was never recorded (e.g. a non-de-interned object) or its entry has been collected.
     */
    public static Object root(Object x) {
        if (x == null) {
            return null;
        }
        Object r = ROOTS.get(x);
        return r != null ? r : x;
    }
}
