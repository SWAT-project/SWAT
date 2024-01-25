package de.uzl.its.swat.logger.inst;

public class GETVALUE_float extends GETVALUE_primitive {

    public GETVALUE_float(float v, int i) {
        super(v, i);
    }

    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_float(this);
    }

    @Override
    public String toString() {
        return "GETVALUE_float v=" + v + ", i=" + i;
    }
}
