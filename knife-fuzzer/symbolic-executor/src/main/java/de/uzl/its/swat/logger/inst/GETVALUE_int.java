package de.uzl.its.swat.logger.inst;

public class GETVALUE_int extends GETVALUE_primitive {

    public GETVALUE_int(int v, int i) {
        super(v, i);
    }

    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_int(this);
    }

    @Override
    public String toString() {
        return "GETVALUE_int v=" + v + ", i=" + i;
    }
}
