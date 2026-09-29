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

    @SuppressWarnings("unused")
    private boolean symbolicContextLoss = false;
    @SuppressWarnings("unused")
    private boolean symbolicPrecisionLoss = false;
    @SuppressWarnings("unused")
    private boolean referenceSemanticChange = false;
    // Whether an approximate model (a sampled Math function) was used. Unlike symbolicPrecisionLoss,
    // which also covers exact UF-based models, solutions from such a trace are only guesses.
    @SuppressWarnings("unused")
    private boolean approximateModel = false;

    public TraceDTO(ArrayList<InputDTO> inputs, ArrayList<BranchDTO> trace, ArrayList<UFDTO> ufs, ArrayList<InvocationDTO> missingInvocations, boolean symbolicContextLoss, boolean symbolicPrecisionLoss, boolean referenceSemanticChange, boolean approximateModel) {
        this.trace = trace;
        this.inputs = inputs;
        this.ufs = ufs;
        this.missingInvocations = missingInvocations;
        this.symbolicContextLoss = symbolicContextLoss;
        this.symbolicPrecisionLoss = symbolicPrecisionLoss;
        this.referenceSemanticChange = referenceSemanticChange;
        this.approximateModel = approximateModel;

    }

    @SuppressWarnings("unused")
    public TraceDTO() {}
}
