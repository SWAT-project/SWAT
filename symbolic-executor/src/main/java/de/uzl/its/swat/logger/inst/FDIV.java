package de.uzl.its.swat.logger.inst;

/**
 * FDIV - Divide float. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fdiv">Java VM
 * specification</a>.
 */
public class FDIV extends Instruction {

    /**
     * Creates a new FDIV instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public FDIV(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitFDIV(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FDIV");
    }
}
