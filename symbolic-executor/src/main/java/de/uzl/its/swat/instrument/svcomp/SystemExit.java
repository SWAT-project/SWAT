package de.uzl.its.swat.instrument.svcomp;

import de.uzl.its.swat.instrument.Intrinsics;

/**
 * Replaces {@code System.exit}, {@code Runtime.exit} and {@code Runtime.halt} in instrumented
 * code. A program that ends this way never returns from main, so without this the trace of the run
 * would never be sent, and the explorer would see a run without any branches (and report SAFE).
 * Like {@link Verifier#assume}, the trace is sent first, then the program ends as it asked to.
 */
public class SystemExit {

    public static void exit(int status) {
        Intrinsics.terminate();
        System.exit(status);
    }

    public static void runtimeExit(Runtime runtime, int status) {
        Intrinsics.terminate();
        runtime.exit(status);
    }

    public static void runtimeHalt(Runtime runtime, int status) {
        Intrinsics.terminate();
        runtime.halt(status);
    }
}
