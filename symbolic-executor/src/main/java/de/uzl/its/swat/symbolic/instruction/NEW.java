package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * NEW - Create new object. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.new">Java VM
 * specification</a>.
 */
public class NEW extends Instruction {

    // The type of the object to be created
    String type;

    // The class index
    public int cIdx;

    /**
     * Creates a NEW instruction.
     *
     * @param iid instruction id.
     * @param type the type of the object to be created
     * @param cIdx the class index
     */
    public NEW(long iid, String type, int cIdx) {
        super(iid);
        this.type = type;
        this.cIdx = cIdx;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitNEW(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("NEW " + type + " " + cIdx);
    }
}
