package de.uzl.its.swat.logger.inst;

/**
 * MONITOREXIT - Exit monitor for object.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.monitorexit">Java VM specification</a>.
 */
public class MONITOREXIT extends Instruction {

    /**
     * Creates a new MONITOREXIT instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public MONITOREXIT(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitMONITOREXIT(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("MONITOREXIT");
    }
}