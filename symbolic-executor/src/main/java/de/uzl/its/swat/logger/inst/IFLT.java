package de.uzl.its.swat.logger.inst;

public class IFLT extends Instruction {
    int label;

    public IFLT(int iid, int mid, int label) {
        super(iid, mid);
        this.label = label;
    }

    public void visit(IVisitor visitor) {
        visitor.visitIFLT(this);
    }

    @Override
    public String toString() {
        return "IFLT iid=" + iid + " mid=" + mid + " label=" + label;
    }
}
