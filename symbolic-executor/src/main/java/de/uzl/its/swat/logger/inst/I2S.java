package de.uzl.its.swat.logger.inst;

/**
 * I2S - Convert int to short.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.i2s">Java VM specification</a>.
 */
public class I2S extends Instruction {

    /**
     * Creates a new I2S instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public I2S(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitI2S(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("I2S");
    }
}
