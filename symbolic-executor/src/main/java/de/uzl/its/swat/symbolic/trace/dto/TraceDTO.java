package de.uzl.its.swat.symbolic.trace.dto;

import java.util.ArrayList;

public class TraceDTO {
    @SuppressWarnings("unused")
    private ArrayList<BranchDTO> trace;

    @SuppressWarnings("unused")
    private ArrayList<InputDTO> inputs;

    @SuppressWarnings("unused")
    private ArrayList<UFDTO> ufs;

    // The methods encountered during execution that could not be modelled symbolically. The
    // superset of all missing invocations; entries with contextLoss=true are the dangerous subset.
    @SuppressWarnings("unused")
    private ArrayList<InvocationDTO> missingInvocations;

    // The values returned by Verifier.nondet*() calls with their call locations (witness data)
    @SuppressWarnings("unused")
    private ArrayList<NondetReturnDTO> nondetReturns;

    // The throwable that escaped the entry method, null if the execution ended normally
    @SuppressWarnings("unused")
    private ViolationDTO violation;

    @SuppressWarnings("unused")
    private boolean symbolicContextLoss = false;
    @SuppressWarnings("unused")
    private boolean symbolicPrecisionLoss = false;
    @SuppressWarnings("unused")
    private boolean referenceSemanticChange = false;
    // Whether an approximate model was used that is precision loss wherever it is used (e.g. object
    // array elements at the concrete index). Unlike symbolicPrecisionLoss, which also covers exact
    // UF-based models, solutions from such a trace are only guesses. Sampled Math functions are
    // recognised per query instead, by their function names.
    @SuppressWarnings("unused")
    private boolean approximateModel = false;
    // A StackOverflowError reached the target's code: the path depends on the JVM's stack size,
    // so a violation of this execution is not a verdict (see StackOverflowGuard)
    @SuppressWarnings("unused")
    private boolean stackOverflow = false;

    public TraceDTO(ArrayList<InputDTO> inputs, ArrayList<BranchDTO> trace, ArrayList<UFDTO> ufs, ArrayList<InvocationDTO> missingInvocations, ArrayList<NondetReturnDTO> nondetReturns, ViolationDTO violation, boolean symbolicContextLoss, boolean symbolicPrecisionLoss, boolean referenceSemanticChange, boolean approximateModel, boolean stackOverflow) {
        this.trace = trace;
        this.inputs = inputs;
        this.ufs = ufs;
        this.missingInvocations = missingInvocations;
        this.nondetReturns = nondetReturns;
        this.violation = violation;
        this.symbolicContextLoss = symbolicContextLoss;
        this.symbolicPrecisionLoss = symbolicPrecisionLoss;
        this.referenceSemanticChange = referenceSemanticChange;
        this.approximateModel = approximateModel;
        this.stackOverflow = stackOverflow;

    }

    @SuppressWarnings("unused")
    public TraceDTO() {}
}
