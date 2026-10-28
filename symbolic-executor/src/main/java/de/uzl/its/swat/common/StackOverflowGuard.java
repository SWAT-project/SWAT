package de.uzl.its.swat.common;

/**
 * Records that the JVM stack overflowed during the execution. Deep recursion in the target plus the
 * frames of the symbolic tracking can exhaust the stack, and a {@link StackOverflowError} can then
 * strike anywhere: in the target's own frames or inside SWAT while an instruction is processed.
 *
 * <ul>
 *   <li>{@link #trackingLost}: a StackOverflowError was raised inside SWAT. The shadow state may be
 *       half updated, so symbolic tracking stops for the rest of the execution; the error is
 *       swallowed, so the target continues as it would without SWAT.
 *   <li>{@link #targetOverflowed}: a StackOverflowError reached the target's code. It is set by the
 *       instrumented exception handler of every call site (a PUTSTATIC, no call) before the
 *       exception is reported to SWAT. The path then depends on the stack size of the JVM, which
 *       the task semantics do not fix, so neither a SAFE nor a VIOLATION found on it can be trusted.
 * </ul>
 *
 * Both end the symbolic tracking and mark the trace as context loss. The fields are written
 * without any call, so that they can be set on an exhausted stack; the class is initialised
 * eagerly by the agent for the same reason. They are global, like the instruction processor whose
 * state they guard.
 */
public final class StackOverflowGuard {

    public static volatile boolean trackingLost = false;
    public static volatile boolean targetOverflowed = false;

    private StackOverflowGuard() {}

    /** Forces the initialisation of the class (see the class comment). */
    public static void init() {}

    /** Whether symbolic tracking has stopped because of a stack overflow. */
    public static boolean isOverflowed() {
        return trackingLost | targetOverflowed;
    }

    /**
     * Returns the StackOverflowError behind {@code t}, or the initialiser error of a class whose
     * static initialiser it broke, or null if {@code t} is not caused by a stack overflow.
     */
    public static Error findStackOverflow(Throwable t) {
        for (int depth = 0; t != null && depth < 16; depth++, t = t.getCause()) {
            if (t instanceof StackOverflowError e) {
                return e;
            }
            if (t instanceof ExceptionInInitializerError e
                    && String.valueOf(e.getMessage()).contains("java.lang.StackOverflowError")) {
                return e;
            }
        }
        return null;
    }

    /** Clears the state; only for tests that run several executions in one JVM. */
    public static void reset() {
        trackingLost = false;
        targetOverflowed = false;
    }
}
