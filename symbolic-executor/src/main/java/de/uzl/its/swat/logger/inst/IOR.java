package de.uzl.its.swat.logger.inst;

/**
 * IOR - Boolean OR int. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.ior">Java VM
 * specification</a>.
 */
public class IOR extends Instruction {

    /**
     * Creates a new IOR instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public IOR(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitIOR(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("IOR");
    }
}
