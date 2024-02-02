package de.uzl.its.swat.logger.inst;

public class GETVALUE_boolean extends GETVALUE_primitive {

    public GETVALUE_boolean(boolean v, int i) {
        super(v, i);
    }

    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_boolean(this);
    }

    @Override
    public String toString() {
        return "GETVALUE_boolean v=" + v + ", i=" + i;
    }
}
