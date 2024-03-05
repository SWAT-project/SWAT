package de.uzl.its.swat.logger.inst;
/**
 * ARRAYLENGTH - Get length of array.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.arraylength">Java VM specification</a>.
 */
public class ARRAYLENGTH extends Instruction {

    /**
     * Creates a new ARRAYLENGTH instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public ARRAYLENGTH(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitARRAYLENGTH(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ARRAYLENGTH");
    }
}
