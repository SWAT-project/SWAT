package de.uzl.its.swat.symbolic.instruction;

/**
 * ARETURN - Return reference from method. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.areturn">Java VM
 * specification</a>.
 */
public class ARETURN extends Instruction {
    /**
     * Creates a new ARETURN instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public ARETURN(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitARETURN(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ARETURN");
    }
}
