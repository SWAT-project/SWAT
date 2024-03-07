package de.uzl.its.swat.symbolic.instruction;

/**
 * MONITORENTER - Enter monitor for object. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.monitorenter">Java
 * VM specification</a>.
 */
public class MONITORENTER extends Instruction {

    /**
     * Creates a new MONITORENTER instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     */
    public MONITORENTER(int iid, int mid) {
        super(iid, mid);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitMONITORENTER(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("MONITORENTER");
    }
}
