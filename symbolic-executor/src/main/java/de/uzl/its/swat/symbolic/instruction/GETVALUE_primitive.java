package de.uzl.its.swat.symbolic.instruction;

public abstract class GETVALUE_primitive extends Instruction {

    // The concrete value/ object to fetch
    public Object v;

    // Symbolic marker (1 if symbolic else 0)
    public int i;

    public GETVALUE_primitive(long iid, Object v, int i) {
        super(iid);
        this.v = v;
        this.i = i;
    }

    public String genericToString(String type) {
        String s = (i == 1) ? "SYMBOLIC " : "CONCRETE ";
        return "[" + s + "VALUE FETCH] " + type + " " + v + " (id: " + iid + ")";
    }
}
