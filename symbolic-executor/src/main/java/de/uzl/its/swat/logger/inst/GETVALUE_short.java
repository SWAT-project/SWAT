package de.uzl.its.swat.logger.inst;

public class GETVALUE_short extends GETVALUE_primitive {
    public GETVALUE_short(short v, int i) {
        super(v, i);
    }

    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_short(this);
    }

    @Override
    public String toString() {
        return "GETVALUE_short v=" + v + ", i=" + i;
    }
}
