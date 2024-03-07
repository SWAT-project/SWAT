package de.uzl.its.swat.symbolic.instruction;

/**
 * IINC - Increment local variable by constant. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.iinc">Java VM
 * specification</a>.
 */
public class IINC extends Instruction {

    // Local variable index
    public int var;
    // the increment amount
    public int increment;

    /**
     * Creates a new IINC instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param var the local variable index.
     * @param increment the increment amount.
     */
    public IINC(int iid, int mid, int var, int increment) {
        super(iid, mid);
        this.var = var;
        this.increment = increment;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitIINC(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IINC [" + var + "] " + increment);
    }
}
