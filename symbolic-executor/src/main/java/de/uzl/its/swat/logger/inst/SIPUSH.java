package de.uzl.its.swat.logger.inst;

/**
 * SIPUSH - Push short. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.sipush">Java VM
 * specification</a>.
 */
public class SIPUSH extends Instruction {

    // The value to push
    public int value;

    /**
     * Creates a new SIPUSH instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param value the value to push
     */
    public SIPUSH(int iid, int mid, int value) {
        super(iid, mid);
        this.value = value;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitSIPUSH(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("SIPUSH " + value);
    }
}
