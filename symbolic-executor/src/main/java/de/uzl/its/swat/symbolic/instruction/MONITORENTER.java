package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * MONITORENTER - Enter monitor for object. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.monitorenter">Java
 * VM specification</a>.
 */
public class MONITORENTER extends Instruction {

    /**
     * Creates a new MONITORENTER instruction.
     *
     * @param iid instruction id.
     */
    public MONITORENTER(long iid) {
        super(iid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitMONITORENTER(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("MONITORENTER");
    }
}
