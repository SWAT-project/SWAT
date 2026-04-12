package de.uzl.its.swat.symbolic.trace;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * This class stores the symbolic trace information gathered during the symbolic execution. After
 * the symbolic execution terminates, the trace is sent to the SymbolicExplorer. The SymbolicTrace
 * should not be accessed directly bt rather through the {@link SymbolicTraceHandler}.
 */
@Getter@Setter
class SymbolicTrace {
    // Stores the values that are tracked symbolically including their bounds
    private final List<InputElement> inputs;
    // Stores formulas that constrain UFs
    private final List<BooleanFormula> ufs;
    // Stores the trace of the current execution. This includes what branches were taken
    // (BranchElement) and the constraints.
    private final List<Element> trace;

    // If true, an invocation occurred that was not instrumented but received symbolic arguments.
    private boolean symbolicContextLoss = false;

    // If true, reference equality semantics may have changed due to comparing user-de-interned strings.
    private boolean referenceSemanticChange = false;

    /** Creates a new SymbolicTrace. */
    public SymbolicTrace() {
        this.inputs = new ArrayList<>();
        this.ufs = new ArrayList<>();
        trace = new ArrayList<>(1024);
    }

    /**
     * Adds an input element to the list of inputs.
     *
     * @param inputElement The value to add.
     */
    protected void addInputElement(InputElement inputElement) {
        this.inputs.add(inputElement);
    }

    /**
     * Adds a UF to the list of UFs.
     *
     * @param uf The UF to add.
     */
    protected void addUF(BooleanFormula uf) {
        this.ufs.add(uf);
    }
    /**
     * Adds an element to the trace.
     *
     * @param element The element to add.
     */
    protected void addTraceElement(Element element) {
        this.trace.add(element);
    }
}
