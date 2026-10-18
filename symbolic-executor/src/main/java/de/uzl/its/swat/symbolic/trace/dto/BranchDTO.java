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
    // See BranchElement: the branch's key and the frame stack change since the previous branch.
    @SuppressWarnings("unused")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String key;
    @SuppressWarnings("unused")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT)
    private int pops;
    @SuppressWarnings("unused")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
    private java.util.List<String> pushes;

    @SuppressWarnings("unused")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT)
    private boolean callback;

    // See BranchElement: a branch the executor adds that the static pre-analysis graph does not have.
    @SuppressWarnings("unused")
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT)
    private boolean choice;

    public BranchDTO choice(boolean choice) {
        this.choice = choice;
        return this;
    }

    public BranchDTO place(String key, int pops, java.util.List<String> pushes, boolean callback) {
        this.key = key;
        this.pops = pops;
        this.pushes = pushes;
        this.callback = callback;
        return this;
    }

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
