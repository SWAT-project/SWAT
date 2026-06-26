package de.uzl.its.swat.symbolic.trace.dto;

public class BranchDTO {
    @SuppressWarnings("unused")
    private final long iid;

    @SuppressWarnings("unused")
    private String constraint;

    @SuppressWarnings("unused")
    private boolean branched;

    @SuppressWarnings("unused")
    private final String type;

    @SuppressWarnings("unused")
    private String inst;

    /**
     * G4: the executor's per-branch precision-loss verdict. Carried so a future explorer-side,
     * CFG-reachability-aware decision can key it by {@link #iid} to a CFG node. The current verdict
     * uses the aggregate {@code symbolicPrecisionLoss} on the TraceDTO (OR of these).
     */
    @SuppressWarnings("unused")
    private boolean precisionLoss;

    public BranchDTO(long iid, String constraint, boolean branched, boolean precisionLoss) {
        this.iid = iid;
        this.constraint = constraint;
        this.branched = branched;
        this.precisionLoss = precisionLoss;
        this.type = "Branch";
    }

    public BranchDTO(long iid, String inst) {
        this.iid = iid;
        this.inst = inst;
        this.type = "Special";
    }
}
