package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;
/**
 * DUP2_X1 - Duplicate the top one or two operand stack values and insert two or three values down.
 * For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dup2_x1">Java VM
 * specification</a>.
 */
public class DUP2_X1 extends Instruction {

    /**
     * Creates a new DUP2_X1 instruction.
     *
     * @param iid instruction id.
     */
    public DUP2_X1(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitDUP2_X1(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DUP2_X1");
    }
}
