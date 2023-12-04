package de.uzl.its.swat.logger.inst;

public abstract class GETVALUE_primitive extends Instruction {
    public Object v;
    public int i;

    public GETVALUE_primitive(Object v, int i) {
        super(-1, -1);
        this.v = v;
        this.i = i;
    }

    public abstract void visit(IVisitor visitor);

    @Override
    public String toString() {
        return "GETVALUE_primitive v=" + v + ", i=" + i;
    }
}
