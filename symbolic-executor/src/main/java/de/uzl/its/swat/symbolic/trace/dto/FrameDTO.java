package de.uzl.its.swat.symbolic.trace.dto;

/** One stack frame of the throwable that ended the execution (see {@link ViolationDTO}). */
public class FrameDTO {
    // The declaring class of the frame (dotted name)
    private String className;
    // The source file of the frame, empty if unknown
    private String sourceFile;
    // The source line of the frame, -1 if unknown
    private int line;

    public FrameDTO(String className, String sourceFile, int line) {
        this.className = className;
        this.sourceFile = sourceFile;
        this.line = line;
    }

    /** Private default constructor for serialization */
    @SuppressWarnings("unused")
    private FrameDTO() {}

    public String getClassName() {
        return className;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return className + "(" + sourceFile + ":" + line + ")";
    }
}
