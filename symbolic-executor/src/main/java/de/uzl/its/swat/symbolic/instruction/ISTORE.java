package de.uzl.its.swat.symbolic.instruction;

/**
 * ISTORE - Store int into local variable. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.istore">Java VM
 * specification</a>.
 */
public class ISTORE extends Instruction {

    // Local variable index
    public int var;

    /**
     * Creates a new ISTORE instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param var local variable index
     */
    public ISTORE(int iid, int mid, int var) {
        super(iid, mid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitISTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ISTORE " + var);
    }
}
