package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * SIPUSH - Push short. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.sipush">Java VM
 * specification</a>.
 */
public class SIPUSH extends Instruction {

    // The value to push
    public int value;

    /**
     * Creates a new SIPUSH instruction.
     *
     * @param iid instruction id.
     * @param value the value to push
     */
    public SIPUSH(long iid, int value) {
        super(iid);
        this.value = value;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitSIPUSH(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("SIPUSH " + value);
    }
}
