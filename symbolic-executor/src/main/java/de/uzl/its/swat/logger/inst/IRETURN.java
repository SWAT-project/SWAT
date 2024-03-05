package de.uzl.its.swat.logger.inst;

/**
 * IRETURN - Return int from method.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.ireturn">Java VM specification</a>.
 */
public class IRETURN extends Instruction {

    /**
     * Creates a new IRETURN instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public IRETURN(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitIRETURN(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IRETURN");
    }
}
