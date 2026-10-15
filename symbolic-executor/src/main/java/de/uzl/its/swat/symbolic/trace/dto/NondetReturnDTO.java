package de.uzl.its.swat.symbolic.trace.dto;

/**
 * A value returned by a {@code Verifier.nondet*()} call together with the location of the call.
 * The explorer turns the sequence of these into the {@code function_return} waypoints of an
 * SV-COMP violation witness.
 */
public class NondetReturnDTO {
    // The Verifier method name, e.g. nondetInt
    private String function;
    // The Java type of the value: boolean|byte|short|char|int|long|float|double|String
    private String type;
    // The value in its witness string form: integers decimal, char as its code point, float/double
    // as Java's Float/Double.toString, boolean true/false, String the raw string.
    private String value;
    // Exact IEEE-754 bits of a float/double value as 0x-prefixed hex, null for other types
    private String bits;
    // The class containing the call (dotted name)
    private String className;
    // The method containing the call as name + descriptor, e.g. main([Ljava/lang/String;)V
    private String method;
    // The SourceFile attribute of that class, empty if unknown
    private String sourceFile;
    // The source line of the call
    private int line;
    // 0-based index among the nondet calls on that source line of that class (evaluation order)
    private int callIndex;

    public NondetReturnDTO(
            String function,
            String type,
            String value,
            String bits,
            String className,
            String method,
            String sourceFile,
            int line,
            int callIndex) {
        this.function = function;
        this.type = type;
        this.value = value;
        this.bits = bits;
        this.className = className;
        this.method = method;
        this.sourceFile = sourceFile;
        this.line = line;
        this.callIndex = callIndex;
    }

    /** Private default constructor for serialization */
    @SuppressWarnings("unused")
    private NondetReturnDTO() {}

    public String getFunction() {
        return function;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getBits() {
        return bits;
    }

    public String getClassName() {
        return className;
    }

    public String getMethod() {
        return method;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public int getLine() {
        return line;
    }

    public int getCallIndex() {
        return callIndex;
    }

    @Override
    public String toString() {
        return "NondetReturnDTO{"
                + function
                + " -> "
                + type
                + " "
                + value
                + (bits == null ? "" : " (" + bits + ")")
                + " at "
                + className
                + "."
                + method
                + "("
                + sourceFile
                + ":"
                + line
                + ")#"
                + callIndex
                + '}';
    }
}
