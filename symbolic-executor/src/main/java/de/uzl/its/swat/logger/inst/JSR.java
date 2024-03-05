package de.uzl.its.swat.logger.inst;

/**
 * JSR - Jump subroutine. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.jsr">Java VM
 * specification</a>.
 */
public class JSR extends Instruction {

    // The jump destination
    int label;

    /**
     * Creates a new JSR instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param label the jump destination
     */
    public JSR(int iid, int mid, int label) {
        super(iid, mid);
        this.label = label;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitJSR(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("JSR " + label);
    }
}
