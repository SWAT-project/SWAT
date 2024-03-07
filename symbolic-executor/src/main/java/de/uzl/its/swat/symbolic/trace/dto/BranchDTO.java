package de.uzl.its.swat.symbolic.trace.dto;

public class BranchDTO {
    @SuppressWarnings("unused")
    private final int iid;

    @SuppressWarnings("unused")
    private String constraint;

    @SuppressWarnings("unused")
    private boolean branched;

    @SuppressWarnings("unused")
    private final String type;

    @SuppressWarnings("unused")
    private String inst;

    public BranchDTO(
            int iid,
            String constraint,
            boolean branched) {
        this.iid = iid;
        this.constraint = constraint;
        this.branched = branched;
        this.type = "Branch";
    }

    public BranchDTO(int iid, String inst) {
        this.iid = iid;
        this.inst = inst;
        this.type = "Special";
    }
}
