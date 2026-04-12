package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * ARRAYLENGTH - Get length of array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.arraylength">Java
 * VM specification</a>.
 */
public class ARRAYLENGTH extends Instruction {

    /**
     * Creates a new ARRAYLENGTH instruction.
     *
     * @param iid instruction id.
     */
    public ARRAYLENGTH(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitARRAYLENGTH(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ARRAYLENGTH");
    }
}
