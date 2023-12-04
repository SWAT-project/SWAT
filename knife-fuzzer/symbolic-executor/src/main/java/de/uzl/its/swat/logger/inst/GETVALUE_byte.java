package de.uzl.its.swat.logger.inst;

public class GETVALUE_byte extends GETVALUE_primitive {
    public GETVALUE_byte(byte v, int i) {
        super(v, i);
    }

    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_byte(this);
    }

    @Override
    public String toString() {
        return "GETVALUE_byte v=" + v + ", i=" + i;
    }
}
