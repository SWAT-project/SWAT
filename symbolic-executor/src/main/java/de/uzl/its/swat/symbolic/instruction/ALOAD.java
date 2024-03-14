package de.uzl.its.swat.symbolic.instruction;
/**
 * ALOAD - Load reference from local variable. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.aload">Java VM
 * specification</a>.
 */
public class ALOAD extends Instruction {

    // Index of the local variable to load.
    public int var;

    /**
     * Creates a new ALOAD instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param var index of the local variable to load.
     */
    public ALOAD(int iid, int mid, int var) {
        super(iid, mid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitALOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ALOAD " + var);
    }
}
