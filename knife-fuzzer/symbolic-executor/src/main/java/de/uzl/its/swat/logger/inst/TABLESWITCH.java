package de.uzl.its.swat.logger.inst;

public class TABLESWITCH extends Instruction {
    public int min;
    public int max;
    public int dflt;
    public int[] labels;
    public int[] values;

    public TABLESWITCH(int iid, int mid, int min, int max, int dflt, int[] labels, int[] values) {
        super(iid, mid);
        this.min = min;
        this.max = max;
        this.dflt = dflt;
        this.labels = labels;
        this.values = values;
    }

    public void visit(IVisitor visitor) {
        visitor.visitTABLESWITCH(this);
    }

    @Override
    public String toString() {
        return "TABLESWITCH iid="
                + iid
                + " mid="
                + mid
                + " min="
                + min
                + " max="
                + max
                + " dflt="
                + dflt
                + " labels="
                + labels
                + " values="
                + values;
    }
}
