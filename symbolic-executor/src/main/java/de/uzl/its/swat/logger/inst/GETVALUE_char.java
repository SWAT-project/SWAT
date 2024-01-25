package de.uzl.its.swat.logger.inst;

public class GETVALUE_char extends GETVALUE_primitive {

    public GETVALUE_char(char v, int i) {
        super(v, i);
    }

    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_char(this);
    }

    @Override
    public String toString() {
        return "GETVALUE_char v=" + v + ", i=" + i;
    }
}
