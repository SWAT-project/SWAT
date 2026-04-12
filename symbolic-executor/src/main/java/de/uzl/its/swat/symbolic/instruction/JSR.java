package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

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
     * @param label the jump destination
     */
    public JSR(long iid, int label) {
        super(iid);
        this.label = label;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
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
