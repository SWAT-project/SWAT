package de.uzl.its.swat.logger.inst;

/**
 * LLOAD - Load long from local variable.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lload">Java VM specification</a>.
 */
public class LLOAD extends Instruction {

    // Local variable index
    public int var;

    /**
     * Creates a new LDIV instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param var local variable index.
     */
    public LLOAD(int iid, int mid, int var) {
        super(iid, mid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitLLOAD(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("LLOAD " + var);
    }
}