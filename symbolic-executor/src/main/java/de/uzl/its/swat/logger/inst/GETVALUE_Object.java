package de.uzl.its.swat.logger.inst;

public class GETVALUE_Object<T> extends Instruction {
    public int v;
    public int i;
    public T val;

    public GETVALUE_Object(int v, T val, int i) {
        super(-1, -1);
        this.v = v;
        this.val = val;
        this.i = i;
    }

    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_Object(this);
    }

    @Override
    public String toString() {
        if (val == null) {
            return "GETVALUE_Object v=" + Integer.toHexString(v) + " i=" + i;
        }
        return "GETVALUE_Object v="
                + Integer.toHexString(v)
                + " type="
                + val.getClass()
                + " value="
                + val
                + " i="
                + i;
    }
}
