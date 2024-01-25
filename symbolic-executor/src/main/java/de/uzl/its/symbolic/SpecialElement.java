package de.uzl.its.symbolic;

public class SpecialElement extends Element {
    int iid;

    @Override
    public int getIid() {
        return iid;
    }

    private String inst;

    public String getInst() {
        return inst;
    }

    public SpecialElement(int iid, String inst) {
        this.iid = iid;
        this.inst = inst;
    }
}
