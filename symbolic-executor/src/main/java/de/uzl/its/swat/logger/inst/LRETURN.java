package de.uzl.its.swat.logger.inst;

/**
 * LRETURN - Return long from method.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lreturn">Java VM specification</a>.
 */
public class LRETURN extends Instruction {

    /**
     * Creates a new LRETURN instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public LRETURN(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLRETURN(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LRETURN");
    }
}