package de.uzl.its.symbolic;

import org.sosy_lab.java_smt.api.BooleanFormula;

public class BranchElement extends Element {
    int iid;

    @Override
    public int getIid() {
        return iid;
    }

    private boolean branched;

    public boolean getBranch() {
        return branched;
    }

    public void setBranch(boolean b) {
        branched = b;
    }

    public int getStackSize() {
        return stackSize;
    }

    public int getLocalsSize() {
        return localsSize;
    }

    public int getCallSize() {
        return callSize;
    }

    private int stackSize;

    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    private int localsSize;

    public void setLocalsSize(int localsSize) {
        this.callSize = localsSize;
    }

    public void setCallSize(int callSize) {
        this.callSize = callSize;
    }

    private int callSize;

    private BooleanFormula pathConstraint;

    public BooleanFormula getPathConstraint() {
        return pathConstraint;
    }

    public void setPathConstraint(BooleanFormula pathConstraint) {
        this.pathConstraint = pathConstraint;
    }

    public BranchElement(boolean branched, BooleanFormula pathConstraint, int iid) {
        this.branched = branched;
        this.pathConstraint = pathConstraint;
        this.iid = iid;
    }

    /** Private default constructor for serialization */
    private BranchElement() {}

    @Override
    public String toString() {
        return "BranchElement{"
                + "branched="
                + branched
                + ", pathConstraint="
                + pathConstraint
                + ", iid="
                + iid
                + '}';
    }
}
