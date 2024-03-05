package de.uzl.its.swat.logger.inst;

/**
 * FRETURN - Return float from method.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.freturn">Java VM specification</a>.
 */
public class FRETURN extends Instruction {

    /**
     * Creates a new FRETURN instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public FRETURN(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitFRETURN(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FRETURN");
    }
}