package de.uzl.its.swat.witness;

import java.math.BigDecimal;
import java.util.Base64;

/**
 * Emits the witness markers that WitnessCreator turns into an SV-COMP violation witness.
 *
 * <p>Two kinds of markers are printed to stdout:
 *
 * <ul>
 *   <li>{@code [WITNESS] <base64>} for every value returned by a {@code Verifier.nondet*()} call.
 *       The payload is {@code
 *       value@@@line@@@className@@@methodDesc@@@callIndex@@@typeDesc@@@sourceFile}, where {@code
 *       callIndex} is the (0-based) index of the call among the nondet calls on the same source
 *       line of the class (bytecode order, which follows the left-to-right evaluation order),
 *       {@code typeDesc} is the JVM descriptor of the returned type and {@code sourceFile} the
 *       SourceFile attribute of the class (empty if unknown).
 *   <li>{@code [WITNESS_TARGET] <base64>} once for the uncaught throwable that ends the run. The
 *       payload is {@code throwableClass@@@frame;frame;...} with frames encoded as {@code
 *       className@@fileName@@line}, innermost first.
 * </ul>
 */
public class Witness {
    private static final String SEPARATOR = "@@@";
    private static final int MAX_TARGET_FRAMES = 64;

    private static void record(
            String value,
            String typeDesc,
            int lineNumber,
            String cname,
            String desc,
            int callIndex,
            String sourceFile) {
        String witness =
                value
                        + SEPARATOR
                        + lineNumber
                        + SEPARATOR
                        + cname
                        + SEPARATOR
                        + desc
                        + SEPARATOR
                        + callIndex
                        + SEPARATOR
                        + typeDesc
                        + SEPARATOR
                        + sourceFile;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(
            boolean value,
            int lineNumber,
            String cname,
            String desc,
            int callIndex,
            String sourceFile) {
        record(String.valueOf(value), "Z", lineNumber, cname, desc, callIndex, sourceFile);
    }

    public static void recordValue(
            byte value, int lineNumber, String cname, String desc, int callIndex, String sourceFile) {
        record(String.valueOf(value), "B", lineNumber, cname, desc, callIndex, sourceFile);
    }

    public static void recordValue(
            char value, int lineNumber, String cname, String desc, int callIndex, String sourceFile) {
        // Chars are transported as their (unsigned) code point, WitnessCreator renders the literal
        record(String.valueOf((int) value), "C", lineNumber, cname, desc, callIndex, sourceFile);
    }

    public static void recordValue(
            short value,
            int lineNumber,
            String cname,
            String desc,
            int callIndex,
            String sourceFile) {
        record(String.valueOf(value), "S", lineNumber, cname, desc, callIndex, sourceFile);
    }

    public static void recordValue(
            int value, int lineNumber, String cname, String desc, int callIndex, String sourceFile) {
        record(String.valueOf(value), "I", lineNumber, cname, desc, callIndex, sourceFile);
    }

    public static void recordValue(
            float value,
            int lineNumber,
            String cname,
            String desc,
            int callIndex,
            String sourceFile) {
        // Use BigDecimal to avoid scientific notation (e.g., -2.022784E-6)
        // but handle special values (NaN, Infinity) which BigDecimal doesn't support
        String valueStr;
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            valueStr = Float.toString(value);
        } else if (value == 0.0f && 1.0f / value < 0.0f) {
            valueStr = "-0.0"; // BigDecimal has no negative zero
        } else {
            valueStr = BigDecimal.valueOf(value).toPlainString();
        }
        record(valueStr, "F", lineNumber, cname, desc, callIndex, sourceFile);
    }

    public static void recordValue(
            long value, int lineNumber, String cname, String desc, int callIndex, String sourceFile) {
        record(String.valueOf(value), "J", lineNumber, cname, desc, callIndex, sourceFile);
    }

    public static void recordValue(
            double value,
            int lineNumber,
            String cname,
            String desc,
            int callIndex,
            String sourceFile) {
        // Use BigDecimal to avoid scientific notation (e.g., 1.23E-10)
        // but handle special values (NaN, Infinity) which BigDecimal doesn't support
        String valueStr;
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            valueStr = Double.toString(value);
        } else if (value == 0.0 && 1.0 / value < 0.0) {
            valueStr = "-0.0"; // BigDecimal has no negative zero
        } else {
            valueStr = BigDecimal.valueOf(value).toPlainString();
        }
        record(valueStr, "D", lineNumber, cname, desc, callIndex, sourceFile);
    }

    public static void recordValue(
            String value,
            int lineNumber,
            String cname,
            String desc,
            int callIndex,
            String sourceFile) {
        record(
                String.valueOf(value),
                "Ljava/lang/String;",
                lineNumber,
                cname,
                desc,
                callIndex,
                sourceFile);
    }

    /**
     * Records the throwable that escaped the entry method and prints its stack trace. The stack
     * trace locates the violating statement for the witness' target waypoint.
     *
     * @param throwable the uncaught throwable
     */
    public static void recordTarget(Throwable throwable) {
        throwable.printStackTrace();
        StringBuilder sb = new StringBuilder(throwable.getClass().getName()).append(SEPARATOR);
        StackTraceElement[] frames = throwable.getStackTrace();
        for (int i = 0; i < frames.length && i < MAX_TARGET_FRAMES; i++) {
            if (i > 0) sb.append(';');
            StackTraceElement frame = frames[i];
            sb.append(frame.getClassName())
                    .append("@@")
                    .append(frame.getFileName() == null ? "" : frame.getFileName())
                    .append("@@")
                    .append(frame.getLineNumber());
        }
        String enc = Base64.getEncoder().encodeToString(sb.toString().getBytes());
        System.out.println("[WITNESS_TARGET] " + enc);
    }
}
