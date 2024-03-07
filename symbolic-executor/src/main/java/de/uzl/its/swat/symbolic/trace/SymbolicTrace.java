package de.uzl.its.swat.symbolic.trace;

import de.uzl.its.swat.symbolic.trace.Element;
import de.uzl.its.swat.symbolic.trace.InputElement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class stores the symbolic trace information gathered during the symbolic execution.
 * After the symbolic execution terminates, the trace is sent to the SymbolicExplorer.
 * The SymbolicTrace should not be accessed directly bt rather through the {@link SymbolicTraceHandler}.
 */
@Getter@Setter
class SymbolicTrace {
    // Stores the values that are tracked symbolically including their bounds
    private final List<InputElement> inputs;
    // Stores the trace of the current execution. This includes what branches were taken (BranchElement) and the constraints.
    private final List<Element> trace;

    /**
     * Creates a new SymbolicTrace.
     */
    public SymbolicTrace() {
        this.inputs = new ArrayList<>();
        trace = new ArrayList<>(1024);
    }

    /**
     * Adds an input element to the list of inputs.
     * @param inputElement The value to add.
     */
    protected void addInputElement(InputElement inputElement) {
        this.inputs.add(inputElement);
    }

    /**
     * Adds an element to the trace.
     * @param element The element to add.
     */
    protected void addTraceElement(Element element) {
        this.trace.add(element);
    }
}
