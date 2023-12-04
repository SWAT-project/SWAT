package de.uzl.its.swat.logger.inst;

public class LOOP_END extends Instruction {

    public LOOP_END(int iid) {
        super(iid, -1);
    }

    public void visit(IVisitor visitor) {
        visitor.visitLOOP_END(this);
    }

    @Override
    public String toString() {
        return "LOOP_END iid=" + iid;
    }
}
