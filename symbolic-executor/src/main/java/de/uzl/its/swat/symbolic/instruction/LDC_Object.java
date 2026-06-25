package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * LDC(_Object) - Push item from run-time constant pool. LDC instructions are handled based on the
 * datatype to load. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lconst_l">Java VM
 * specification</a>.
 */
public class LDC_Object extends Instruction {

    // The identity hash of the loaded object (kept for address, NULL, and debug)
    public int c;

    // The loaded constant object (the canonical-registry key, by reference identity)
    public Object object;

    /**
     * Creates a new LDC_Object instruction.
     *
     * @param iid instruction id.
     * @param c the identity hash of the loaded object
     * @param object the loaded constant object (the registry key)
     */
    public LDC_Object(long iid, int c, Object object) {
        super(iid);
        this.c = c;
        this.object = object;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitLDC_Object(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LDC [Ljava/lang/Object; @" + Integer.toHexString(c) + "]");
    }
}
