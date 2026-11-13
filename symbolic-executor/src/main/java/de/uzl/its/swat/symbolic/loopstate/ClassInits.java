package de.uzl.its.swat.symbolic.loopstate;

import java.util.Set;
import java.util.TreeSet;

/**
 * The application classes whose initialization has started. The instrumentation calls
 * {@link #started} first thing in every application class's static initializer (and adds one to a
 * class that has none), so the set is exact: a class missing from it is uninitialized, its static
 * fields still hold their defaults and its initializer is still to run.
 */
public final class ClassInits {

    private static final Set<String> started = new TreeSet<>();

    private ClassInits() {}

    public static synchronized void started(String internalName) {
        started.add(internalName);
    }

    /** The classes, in name order. */
    static synchronized String[] snapshot() {
        return started.toArray(new String[0]);
    }
}
