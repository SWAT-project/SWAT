package de.uzl.its.swat.logger.inst;

public class GETVALUE_long extends GETVALUE_primitive {
    public GETVALUE_long(long v, int i) {
        super(v, i);
    }

    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_long(this);
    }

    @Override
    public String toString() {
        return "GETVALUE_long v=" + v + ", i=" + i;
    }
}
