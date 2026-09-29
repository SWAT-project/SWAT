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
    @SuppressWarnings("unused")
    private boolean concreteOnly;

    public BranchDTO(long iid, String constraint, boolean branched) {
        this(iid, constraint, branched, false);
    }

    public BranchDTO(long iid, String constraint, boolean branched, boolean concreteOnly) {
        this.iid = iid;
        this.constraint = constraint;
        this.branched = branched;
        this.concreteOnly = concreteOnly;
        this.type = "Branch";
    }

    public BranchDTO(long iid, String inst) {
        this.iid = iid;
        this.inst = inst;
        this.type = "Special";
    }
}
