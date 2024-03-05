package de.uzl.its.swat.logger.inst;

/**
 * ANEWARRAY - Create new array of reference. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.anewarray">Java VM
 * specification</a>.
 */
public class ANEWARRAY extends Instruction {
    // Type of the array to create.
    public String type;

    /**
     * Creates a new ANEWARRAY instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param type type of the array to create.
     */
    public ANEWARRAY(int iid, int mid, String type) {
        super(iid, mid);
        this.type = type;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitANEWARRAY(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("ANEWARRAY " + type);
    }
}
