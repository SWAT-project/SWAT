package de.uzl.its.swat.logger.inst;

/**
 * F2D - Convert float to double. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.f2d">Java VM
 * specification</a>.
 */
public class F2D extends Instruction {

    /**
     * Creates a new F2D instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public F2D(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitF2D(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("F2D");
    }
}
