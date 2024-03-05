package de.uzl.its.swat.logger.inst;
/**
 * BIPUSH - Push byte.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.bipush">Java VM specification</a>.
 */

public class BIPUSH extends Instruction {
    // The value to be pushed
    public int value;

    /**
     * Creates a new BIPUSH instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param value the value to be pushed.
     */
    public BIPUSH(int iid, int mid, int value) {
        super(iid, mid);
        this.value = value;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitBIPUSH(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */

    @Override
    public String toString() {
        return genericToString("BIPUSH " + value);
    }
}
