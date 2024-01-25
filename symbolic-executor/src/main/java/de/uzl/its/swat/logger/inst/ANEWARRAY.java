package de.uzl.its.swat.logger.inst;

public class ANEWARRAY extends Instruction {
    public String type;

    public ANEWARRAY(int iid, int mid, String type) {
        super(iid, mid);
        this.type = type;
    }

    public void visit(IVisitor visitor) {
        visitor.visitANEWARRAY(this);
    }

    @Override
    public String toString() {
        return "ANEWARRAY iid=" + iid + " mid=" + mid + " type=" + type;
    }
}
