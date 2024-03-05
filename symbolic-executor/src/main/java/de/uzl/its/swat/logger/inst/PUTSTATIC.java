package de.uzl.its.swat.logger.inst;

/**
 * PUTSTATIC - Set static field in class.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.putstatic">Java VM specification</a>.
 */
public class PUTSTATIC extends Instruction {

    // The class index
    public int cIdx;

    // The field index
    public int fIdx;

    // The field descriptor
    public String desc;


    /**
     * Creates a new PUTSTATIC instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param cIdx class index.
     * @param fIdx field index.
     * @param desc field descriptor.
     */
    public PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        super(iid, mid);
        this.cIdx = cIdx;
        this.fIdx = fIdx;
        this.desc = desc;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitPUTSTATIC(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("PUTSTATIC [" + cIdx + ", " + fIdx + "] " + desc);
    }
}