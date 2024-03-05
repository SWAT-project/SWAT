package de.uzl.its.swat.logger.inst;

/**
 * IFNULL - Branch if reference is null. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.ifnull">Java VM
 * specification</a>.
 */
public class IFNULL extends Instruction {
    // The jump destination
    int label;

    /**
     * Creates a new IFNULL instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param label the jump destination
     */
    public IFNULL(int iid, int mid, int label) {
        super(iid, mid);
        this.label = label;
    }

    public void visit(IVisitor visitor) {
        visitor.visitIFNULL(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IFNULL " + label);
    }
}
