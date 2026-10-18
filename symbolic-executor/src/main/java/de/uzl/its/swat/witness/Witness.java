package de.uzl.its.swat.witness;

import de.uzl.its.swat.symbolic.trace.dto.FrameDTO;
import de.uzl.its.swat.symbolic.trace.dto.NondetReturnDTO;
import de.uzl.its.swat.symbolic.trace.dto.ViolationDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records the data an SV-COMP violation witness is built from. The data travels to the explorer as
 * part of the trace ({@code TraceDTO.nondetReturns} and {@code TraceDTO.violation}); nothing is
 * printed for it.
 *
 * <ul>
 *   <li>{@link #recordValue} is invoked by the SV-COMP instrumentation right after every {@code
 *       Verifier.nondet*()} call with the returned value and the location of the call: the class,
 *       the calling method (name + descriptor), the class' SourceFile attribute, the line and the
 *       index of the call among the nondet calls on that line (bytecode order, which follows the
 *       left-to-right evaluation order).
 *   <li>{@link #recordThrow} is invoked by the SV-COMP instrumentation before every {@code athrow}
 *       of the task. An {@code AssertionError} is the violation of an {@code assert}, and it counts
 *       where it is thrown, whether or not it later escapes the entry method (a {@code finally}
 *       that calls {@code System.exit}, returns or throws would otherwise hide it).
 *   <li>{@link #recordTarget} is invoked by the surrounding try/catch of the entry method with the
 *       throwable that escaped it; its stack trace locates the violating statement.
 * </ul>
 *
 * <p>The state is static: every execution of the target runs in its own JVM, and the recording
 * calls may happen before a thread context exists (e.g. in static initializers).
 */
public final class Witness {
    /** Upper bound on the recorded stack frames of the violation (after dropping JDK/SWAT frames). */
    static final int MAX_FRAMES = 32;

    /** Class name prefixes of frames that cannot be the violating statement of the task. */
    private static final String[] SKIPPED_FRAME_PREFIXES = {
        "java.", "javax.", "jdk.", "sun.", "com.sun.", "de.uzl.its.swat.", "shadow.swat."
    };

    private static final List<NondetReturnDTO> nondetReturns =
            Collections.synchronizedList(new ArrayList<>());
    private static volatile ViolationDTO violation = null;
    // The AssertionError reported by recordThrow, if any (the first one thrown in the task)
    private static volatile Throwable thrownAssertion = null;

    private Witness() {}

    private static void record(
            String function,
            String type,
            String value,
            String bits,
            String cname,
            String method,
            String sourceFile,
            int line,
            int callIndex) {
        nondetReturns.add(
                new NondetReturnDTO(
                        function,
                        type,
                        value,
                        bits,
                        cname.replace('/', '.'),
                        method,
                        sourceFile == null ? "" : sourceFile,
                        line,
                        callIndex));
    }

    public static void recordValue(
            boolean value, String function, String cname, String method, String sourceFile, int line, int callIndex) {
        record(function, "boolean", String.valueOf(value), null, cname, method, sourceFile, line, callIndex);
    }

    public static void recordValue(
            byte value, String function, String cname, String method, String sourceFile, int line, int callIndex) {
        record(function, "byte", String.valueOf(value), null, cname, method, sourceFile, line, callIndex);
    }

    public static void recordValue(
            short value, String function, String cname, String method, String sourceFile, int line, int callIndex) {
        record(function, "short", String.valueOf(value), null, cname, method, sourceFile, line, callIndex);
    }

    public static void recordValue(
            char value, String function, String cname, String method, String sourceFile, int line, int callIndex) {
        // Chars travel as their (unsigned) code point; the witness renders the literal
        record(function, "char", String.valueOf((int) value), null, cname, method, sourceFile, line, callIndex);
    }

    public static void recordValue(
            int value, String function, String cname, String method, String sourceFile, int line, int callIndex) {
        record(function, "int", String.valueOf(value), null, cname, method, sourceFile, line, callIndex);
    }

    public static void recordValue(
            long value, String function, String cname, String method, String sourceFile, int line, int callIndex) {
        record(function, "long", String.valueOf(value), null, cname, method, sourceFile, line, callIndex);
    }

    public static void recordValue(
            float value, String function, String cname, String method, String sourceFile, int line, int callIndex) {
        // Float.toString is exact enough to round-trip (incl. NaN, Infinity, -0.0); the raw bits
        // let the witness reproduce the value bit for bit.
        record(
                function,
                "float",
                Float.toString(value),
                "0x" + Integer.toHexString(Float.floatToRawIntBits(value)),
                cname,
                method,
                sourceFile,
                line,
                callIndex);
    }

    public static void recordValue(
            double value, String function, String cname, String method, String sourceFile, int line, int callIndex) {
        record(
                function,
                "double",
                Double.toString(value),
                "0x" + Long.toHexString(Double.doubleToRawLongBits(value)),
                cname,
                method,
                sourceFile,
                line,
                callIndex);
    }

    public static void recordValue(
            String value, String function, String cname, String method, String sourceFile, int line, int callIndex) {
        record(function, "String", value == null ? "" : value, null, cname, method, sourceFile, line, callIndex);
    }

    /**
     * Records the throwable that escaped the entry method and prints its stack trace (the explorer
     * derives the verdict from that output). The frames locate the violating statement for the
     * witness' target waypoint.
     *
     * @param throwable the uncaught throwable
     */
    public static void recordTarget(Throwable throwable) {
        if (throwable == thrownAssertion) {
            // Reported when it was thrown
            return;
        }
        throwable.printStackTrace();
        violation = describe(throwable);
    }

    /**
     * Reports the first {@code AssertionError} thrown by the task: prints its stack trace (the
     * explorer derives the verdict from that output) and records it as the violation. Under the
     * SV-COMP semantics the assertion fails where it is thrown, so the report must not depend on the
     * error escaping the entry method. Other throwables are left to {@link #recordTarget}; a rethrow
     * of the reported error (e.g. at the end of a {@code finally}) is ignored.
     *
     * @param throwable the throwable about to be thrown
     */
    public static void recordThrow(Throwable throwable) {
        if (!(throwable instanceof AssertionError) || thrownAssertion != null) {
            return;
        }
        synchronized (Witness.class) {
            if (thrownAssertion != null) return;
            thrownAssertion = throwable;
        }
        throwable.printStackTrace();
        violation = describe(throwable);
    }

    /**
     * Builds the violation entry for a throwable: its class name and its stack frames, innermost
     * first, without the JDK and SWAT frames (which cannot be the violating statement), capped at
     * {@link #MAX_FRAMES}.
     *
     * @param throwable the throwable
     * @return the violation entry
     */
    public static ViolationDTO describe(Throwable throwable) {
        List<FrameDTO> frames = new ArrayList<>();
        for (StackTraceElement frame : throwable.getStackTrace()) {
            if (frames.size() >= MAX_FRAMES) break;
            if (isSkipped(frame.getClassName())) continue;
            frames.add(
                    new FrameDTO(
                            frame.getClassName(),
                            frame.getFileName() == null ? "" : frame.getFileName(),
                            frame.getLineNumber()));
        }
        return new ViolationDTO(throwable.getClass().getName(), frames);
    }

    private static boolean isSkipped(String className) {
        for (String prefix : SKIPPED_FRAME_PREFIXES) {
            if (className.startsWith(prefix)) return true;
        }
        return false;
    }

    /** The recorded nondet return values in execution order (a copy). */
    public static ArrayList<NondetReturnDTO> getNondetReturns() {
        synchronized (nondetReturns) {
            return new ArrayList<>(nondetReturns);
        }
    }

    /** The recorded violation, or null if no throwable escaped the entry method (yet). */
    public static ViolationDTO getViolation() {
        return violation;
    }

    /** Clears the recorded data (for tests; a target execution never needs this). */
    public static void reset() {
        nondetReturns.clear();
        violation = null;
        thrownAssertion = null;
    }
}
