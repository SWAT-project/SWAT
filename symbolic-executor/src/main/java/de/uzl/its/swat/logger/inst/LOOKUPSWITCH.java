package de.uzl.its.swat.logger.inst;

/**
 * LOOKUPSWITCH - Access jump table by key match and jump.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lookupswitch">Java VM specification</a>.
 */
public class LOOKUPSWITCH extends Instruction {

    // Beginning of the default handler block.
    public int dflt;

    // The values of the keys.
    public int[] keys;

    // Beginnings of the handler blocks.
    public int[] labels;

    /**
     * Creates a new LOOKUPSWITCH instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param dflt beginning of the default handler block.
     * @param keys the values of the keys.
     * @param labels beginnings of the handler blocks.
     */
    public LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels) {
        super(iid, mid);
        this.dflt = dflt;
        this.keys = keys;
        this.labels = labels;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLOOKUPSWITCH(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LOOKUPSWITCH keys: " + keys + " labels: " + labels + " dflt: " + dflt);
    }
}