package de.uzl.its.swat.logger.inst;

public class IFNONNULL extends Instruction {
    int label;

    public IFNONNULL(int iid, int mid, int label) {
        super(iid, mid);
        this.label = label;
    }

    public void visit(IVisitor visitor) {
        visitor.visitIFNONNULL(this);
    }

    @Override
    public String toString() {
        return "IFNONNULL iid=" + iid + " mid=" + mid + " label=" + label;
    }
}
