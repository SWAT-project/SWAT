package de.uzl.its.swat.symbolic.instruction;

/**
 * ACONST_NULL - Push null. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.aconst_null">Java
 * VM specification</a>.
 */
public class ACONST_NULL extends Instruction {

    /**
     * Creates a new ACONST_NULL instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public ACONST_NULL(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitACONST_NULL(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ACONST_NULL");
    }
}
