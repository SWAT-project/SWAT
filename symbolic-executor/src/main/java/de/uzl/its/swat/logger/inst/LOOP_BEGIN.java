package de.uzl.its.swat.logger.inst;

public class LOOP_BEGIN extends Instruction {

    public LOOP_BEGIN(int iid) {
        super(iid, -1);
    }

    public void visit(IVisitor visitor) {
        visitor.visitLOOP_BEGIN(this);
    }

    @Override
    public String toString() {
        return "LOOP_BEGIN iid=" + iid;
    }
}
