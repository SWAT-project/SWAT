package de.uzl.its.swat.symbolic.instruction;

/**
 * TABLESWITCH - Access jump table by index and jump. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.tableswitch">Java
 * VM specification</a>.
 */
public class TABLESWITCH extends Instruction {

    // The min value
    public int min;
    // The max value
    public int max;
    // The default label
    public int dflt;
    // The jump destination labels
    public int[] labels;
    // The (table) lookup values
    public int[] values;

    /**
     * Creates a new TABLESWITCH instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param min min value.
     * @param max max value.
     * @param dflt the default jump label.
     * @param labels the conditional jump labels.
     * @param values the conditions/ keys.
     */
    public TABLESWITCH(int iid, int mid, int min, int max, int dflt, int[] labels, int[] values) {
        super(iid, mid);
        this.min = min;
        this.max = max;
        this.dflt = dflt;
        this.labels = labels;
        this.values = values;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitTABLESWITCH(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString(
                "TABLESWITCH ["
                        + min
                        + "-"
                        + max
                        + "] labels: "
                        + labels
                        + " values: "
                        + values
                        + " dflt: "
                        + dflt);
    }
}
