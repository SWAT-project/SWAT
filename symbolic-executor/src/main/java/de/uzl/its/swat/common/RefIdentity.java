package de.uzl.its.swat.common;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;

/**
 * Bookkeeping that lets {@link UtilInstrumented#refEquals} reproduce Java's reference identity
 * although the NoCache transformer de-interns string literals and the boxing caches.
 *
 * <p>Every object that SWAT creates in place of a shared instance (a string literal, a {@code
 * valueOf} result, an {@code intern()} result) is registered here. Two such objects are one
 * instance in the original program exactly when their values are equal (and, for boxes, the value
 * is in the cache range). Objects that are not registered keep their own identity.
 *
 * <p>This class is not instrumented and its methods are ignored invocations: their results only
 * depend on where an object was created, which the control flow already determines, so the
 * symbolic state receives them as concrete values.
 */
@SuppressWarnings("unused")
public final class RefIdentity {

    private static final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private static final HashSet<Key> deInterned = new HashSet<>();

    private RefIdentity() {}

    /** Registers {@code copy} as standing for the shared instance of its value. */
    public static void markDeInterned(Object copy) {
        if (copy == null) return;
        synchronized (deInterned) {
            expunge();
            deInterned.add(new Key(copy, queue));
        }
    }

    public static boolean isDeInterned(Object o) {
        if (o == null) return false;
        synchronized (deInterned) {
            return deInterned.contains(new Key(o, null));
        }
    }

    /**
     * Whether {@code s} is, in the original program, the canonical (interned) instance of its value:
     * a de-interned copy, or a pooled string that reached the program without passing the NoCache
     * transformer (e.g. a literal returned by the JDK). The pool lookup interns a fresh copy rather
     * than {@code s}, so it never turns {@code s} itself into the pooled instance.
     */
    public static boolean isCanonicalString(String s) {
        return isDeInterned(s) || isPooledOnly(s);
    }

    /** Whether {@code s} is an unregistered string that is the pooled instance of its value. */
    public static boolean isPooledOnly(String s) {
        return !isDeInterned(s) && new String(s).intern() == s;
    }

    /**
     * Whether {@code o} is, in the original program, a cached box: a de-interned {@code valueOf}
     * copy (cached if its value is in the cache range, which the caller checks), or the cached
     * instance itself.
     */
    public static boolean isCanonicalBox(Object o) {
        if (isDeInterned(o)) return true;
        if (o instanceof Integer i) return i == Integer.valueOf(i.intValue());
        if (o instanceof Long l) return l == Long.valueOf(l.longValue());
        if (o instanceof Short s) return s == Short.valueOf(s.shortValue());
        if (o instanceof Byte b) return b == Byte.valueOf(b.byteValue());
        if (o instanceof Character c) return c == Character.valueOf(c.charValue());
        if (o instanceof Boolean b) return b == Boolean.valueOf(b.booleanValue());
        return false;
    }

    /**
     * {@code s.intern() == s} with the JVM's own side effect: an unregistered string whose value is
     * not pooled yet becomes the pooled instance. A registered copy never does, its value is pooled.
     */
    public static boolean internsToItself(String s) {
        return !isDeInterned(s) && s.intern() == s;
    }

    /** Plain reference identity, kept out of the symbolic state's view of {@code IF_ACMP}. */
    public static boolean same(Object a, Object b) {
        return a == b;
    }

    /**
     * Marks a reference comparison whose outcome depends on the values of {@code a} and {@code b}
     * in a way SWAT does not encode. The symbolic state records a reference semantic change when an
     * argument is symbolic.
     */
    public static void valueDependentIdentity(Object a, Object b) {}

    private static void expunge() {
        Object k;
        while ((k = queue.poll()) != null) {
            deInterned.remove(k);
        }
    }

    private static final class Key extends WeakReference<Object> {
        private final int hash;

        Key(Object referent, ReferenceQueue<Object> q) {
            super(referent, q);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key k)) return false;
            Object referent = get();
            return referent != null && referent == k.get();
        }
    }
}
