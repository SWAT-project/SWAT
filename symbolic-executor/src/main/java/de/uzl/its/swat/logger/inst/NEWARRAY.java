package de.uzl.its.swat.logger.inst;

public class NEWARRAY extends Instruction {
    public int atype;

    public NEWARRAY(int iid, int mid, int atype) {
        super(iid, mid);
        this.atype = atype;
    }

    public void visit(IVisitor visitor) {
        visitor.visitNEWARRAY(this);
    }

    @Override
    public String toString() {
        String type = "";
        switch (atype) {
            case 4 -> type = "T_BOOLEAN";
            case 5 -> type = "T_CHAR";
            case 6 -> type = "T_FLOAT";
            case 7 -> type = "T_DOUBLE";
            case 8 -> type = "T_BYTE";
            case 9 -> type = "T_SHORT";
            case 10 -> type = "T_INT";
            case 11 -> type = "T_LONG";
        }
        return "NEWARRAY iid=" + iid + " mid=" + mid + " atype=" + atype + " (" + type + ")";
    }
}
