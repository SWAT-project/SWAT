package de.uzl.its.swat.logger.inst;

/**
 * FLOAD - Load float from local variable. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fload">Java VM
 * specification</a>.
 */
public class FLOAD extends Instruction {
    // Local variable index to load
    public int var;

    /**
     * Creates a new FLOAD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param var local variable index to load
     */
    public FLOAD(int iid, int mid, int var) {
        super(iid, mid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitFLOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FLOAD " + var);
    }
}
