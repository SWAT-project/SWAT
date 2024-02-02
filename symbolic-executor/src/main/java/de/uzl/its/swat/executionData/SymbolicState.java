package de.uzl.its.swat.executionData;

import de.uzl.its.symbolic.Element;
import de.uzl.its.symbolic.InputElement;
import java.util.ArrayList;
import java.util.List;

public class SymbolicState {
    private final List<InputElement> inputs;
    private final List<Element> trace;
    private int index;

    public SymbolicState() {

        this.inputs = new ArrayList<>();
        trace = new ArrayList<>(1024);
        this.index = 0;
    }

    public void addInputElement(InputElement inputElement) {
        this.inputs.add(inputElement);
    }

    public List<InputElement> getInputs() {
        return inputs;
    }

    public List<Element> getTrace() {
        return trace;
    }

    public void addTraceElement(Element element) {
        trace.add(element);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
