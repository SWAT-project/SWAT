package de.uzl.its.swat.symbolic.instruction;

/**
 * DRETURN - Return double from method. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.dreturn">Java VM
 * specification</a>.
 */
public class DRETURN extends Instruction {

    /**
     * Creates a new DRETURN instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public DRETURN(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitDRETURN(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("DRETURN");
    }
}
