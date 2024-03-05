package de.uzl.its.swat.logger.inst;

/**
 * LDC(_Object) - Push item from run-time constant pool.
 * LDC instructions are handled based on the datatype to load.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lconst_l">Java VM specification</a>.
 */
public class LDC_Object extends Instruction {

    // The address of the object that's loaded (?)
    public int c;

    /**
     * Creates a new LDC_long instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param c the address of the object that's loaded
     */
    public LDC_Object(int iid, int mid, int c) {
        super(iid, mid);
        this.c = c;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLDC_Object(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LDC [Ljava/lang/Object; @" + Integer.toHexString(c) + "]");
    }
}