package de.uzl.its.swat.logger.inst;

public class GETVALUE_double extends GETVALUE_primitive {

    public GETVALUE_double(double v, int i) {
        super(v, i);
    }

    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_double(this);
    }

    @Override
    public String toString() {
        return "GETVALUE_double v=" + v + ", i=" + i;
    }
}
