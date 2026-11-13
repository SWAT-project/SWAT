package de.uzl.its.swat.instrument.svcomp;

import de.uzl.its.swat.instrument.Intrinsics;

/**
 * Replaces {@code System.exit}, {@code Runtime.exit} and {@code Runtime.halt} in instrumented
 * code. A program that ends this way never returns from main, so without this the trace of the run
 * would never be sent, and the explorer would see a run without any branches (and report SAFE).
 * Like {@link Verifier#assume}, the trace is sent first, then the program ends as it asked to.
 *
 * <p>This is the primary exit path. It runs on the target thread and terminates the execution
 * like the end of main does (incl. the flush of the last instruction), and it is the only path for
 * {@code Runtime.halt}, which skips shutdown hooks. {@link Intrinsics#terminateOnShutdown} (the
 * shutdown hook of the agent) remains as the fallback for exits that do not pass through an
 * instrumented call site, e.g. {@code System.exit} called from uninstrumented library code or by
 * reflection; after this path it finds no active execution and does nothing.
 *
 * <p>An assertion that fails before the exit, e.g. in the {@code try} of a {@code finally} that
 * exits, has already been reported when its error was thrown ({@link
 * de.uzl.its.swat.witness.Witness#recordThrow}).
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
