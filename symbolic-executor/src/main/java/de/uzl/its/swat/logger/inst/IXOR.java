package de.uzl.its.swat.logger.inst;

/**
 * IXOR - Boolean XOR int.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.ixor">Java VM specification</a>.
 */
public class IXOR extends Instruction {

    /**
     * Creates a new IXOR instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public IXOR(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitIXOR(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IXOR");
    }
}