package de.uzl.its.swat.symbolic.trace.dto;

import java.util.List;

/**
 * The throwable that escaped the entry method, recorded by the surrounding try/catch of main. The
 * explorer derives the target waypoint of a violation witness from the innermost frame that lies
 * in the task sources.
 */
public class ViolationDTO {
    // The class name of the throwable, e.g. java.lang.AssertionError
    private String throwable;
    // The stack frames, innermost first; JDK and SWAT frames are left out
    private List<FrameDTO> frames;

    public ViolationDTO(String throwable, List<FrameDTO> frames) {
        this.throwable = throwable;
        this.frames = frames;
    }

    /** Private default constructor for serialization */
    @SuppressWarnings("unused")
    private ViolationDTO() {}

    public String getThrowable() {
        return throwable;
    }

    public List<FrameDTO> getFrames() {
        return frames;
    }

    @Override
    public String toString() {
        return "ViolationDTO{" + throwable + " " + frames + '}';
    }
}
