package de.uzl.its.swat.logger.inst;

/**
 * LXOR - Boolean XOR long.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lxor">Java VM specification</a>.
 */
public class LXOR extends Instruction {

    /**
     * Creates a new LXOR instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public LXOR(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLXOR(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LXOR");
    }
}