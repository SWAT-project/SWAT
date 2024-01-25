package de.uzl.its.dto;

public class BranchDTO {
    @SuppressWarnings("unused")
    private final int iid;

    @SuppressWarnings("unused")
    private String constraint;

    @SuppressWarnings("unused")
    private boolean branched;

    @SuppressWarnings("unused")
    private int stackSize;

    @SuppressWarnings("unused")
    private int localsSize;

    @SuppressWarnings("unused")
    private int callSize;

    @SuppressWarnings("unused")
    private final String type;

    @SuppressWarnings("unused")
    private String inst;

    public BranchDTO(
            int iid,
            String constraint,
            boolean branched,
            int stackSize,
            int localsSize,
            int callSize) {
        this.iid = iid;
        this.constraint = constraint;
        this.branched = branched;
        this.stackSize = stackSize;
        this.localsSize = localsSize;
        this.callSize = callSize;
        this.type = "Branch";
    }

    public BranchDTO(int iid, String inst) {
        this.iid = iid;
        this.inst = inst;
        this.type = "Special";
    }
}
