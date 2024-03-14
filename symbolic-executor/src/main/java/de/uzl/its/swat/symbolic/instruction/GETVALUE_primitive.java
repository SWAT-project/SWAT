package de.uzl.its.swat.symbolic.instruction;

public abstract class GETVALUE_primitive extends Instruction {

    // The concrete value/ object to fetch
    public Object v;

    // Symbolic marker (1 if symbolic else 0)
    public int i;

    public GETVALUE_primitive(Object v, int i) {
        super(-1, -1);
        this.v = v;
        this.i = i;
    }

    public abstract void accept(IVisitor visitor);

    public String genericToString(String type) {
        String s = (i == 1) ? "SYMBOLIC " : "";
        return "[" + s + "VALUE FETCH] " + type + " " + v;
    }
}
