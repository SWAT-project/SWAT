package de.uzl.its.swat.logger.inst;

/**
 * RET - Return from subroutine.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.ret">Java VM specification</a>.
 */
public class RET extends Instruction {

    // Local variable index to the return address
    int var;

    /**
     * Creates a new RET instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param var local variable index to the return address
     */
    public RET(int iid, int mid, int var) {
        super(iid, mid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitRET(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("RET " + var);
    }
}