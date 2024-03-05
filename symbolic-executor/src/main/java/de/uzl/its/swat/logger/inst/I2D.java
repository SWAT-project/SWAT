package de.uzl.its.swat.logger.inst;

/**
 * I2D - Convert int to double. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.i2d">Java VM
 * specification</a>.
 */
public class I2D extends Instruction {

    /**
     * Creates a new I2D instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public I2D(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitI2D(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("I2D");
    }
}
