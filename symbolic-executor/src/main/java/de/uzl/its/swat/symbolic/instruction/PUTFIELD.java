package de.uzl.its.swat.symbolic.instruction;

/**
 * PUTFIELD - Set field in object. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.putfield">Java VM
 * specification</a>.
 */
public class PUTFIELD extends Instruction {

    // The class index
    public int cIdx;

    // The field index
    public int fIdx;

    // The field descriptor
    public String desc;

    /**
     * Creates a new PUTFIELD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    public PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        super(iid, mid);
        this.cIdx = cIdx;
        this.fIdx = fIdx;
        this.desc = desc;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitPUTFIELD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("PUTFIELD [" + cIdx + ", " + fIdx + "] " + desc);
    }
}
