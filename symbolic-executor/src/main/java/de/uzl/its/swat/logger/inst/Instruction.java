package de.uzl.its.swat.logger.inst;

import java.io.Serializable;

public abstract class Instruction implements Serializable {
    public int iid;
    public int mid;

    public abstract void visit(IVisitor visitor);

    public Instruction(int iid, int mid) {
        this.iid = iid;
        this.mid = mid;
    }
}
