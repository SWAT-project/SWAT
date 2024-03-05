package de.uzl.its.swat.logger.inst;

/**
 * RETURN - Return void from method. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.return">Java VM
 * specification</a>.
 */
public class RETURN extends Instruction {

    /**
     * Creates a new RETURN instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public RETURN(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitRETURN(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("RETURN");
    }
}
