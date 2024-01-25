package de.uzl.its.swat.logger.inst;

public class INVOKEDYNAMIC extends Instruction {
    public String owner;
    public String name;
    public String desc;
    public String lambda;

    public INVOKEDYNAMIC(int iid, int mid, String owner, String name, String desc, String lambda) {
        super(iid, mid);
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.lambda = lambda;
    }

    public void visit(IVisitor visitor) {
        visitor.visitINVOKEDYNAMIC(this);
    }

    @Override
    public String toString() {
        return "INVOKEDYNAMIC iid="
                + iid
                + " mid="
                + mid
                + " owner="
                + owner
                + " name="
                + name
                + " desc="
                + desc
                + " lambda="
                + lambda;
    }
}
