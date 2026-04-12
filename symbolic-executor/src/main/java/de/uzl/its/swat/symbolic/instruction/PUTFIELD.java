package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * PUTFIELD - Set field in object. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.putfield">Java VM
 * specification</a>.
 */
public class PUTFIELD extends Instruction {

    // The class index
    public int cIdx;

    // The field name
    public String name;

    // The field descriptor
    public String desc;

    /**
     * Creates a new PUTFIELD instruction.
     *
     * @param iid instruction id.
     * @param cIdx class index.
     * @param name field name.
     * @param desc field descriptor.
     */
    public PUTFIELD(long iid, int cIdx, String name, String desc) {
        super(iid);
        this.cIdx = cIdx;
        this.name = name;
        this.desc = desc;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitPUTFIELD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("PUTFIELD [" + cIdx + ", " + name + "] " + desc);
    }
}
