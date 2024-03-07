package de.uzl.its.swat.symbolic.instruction;

/**
 * INSTANCEOF - Determine if object is of given type. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.instanceof">Java
 * VM specification</a>.
 */
public class INSTANCEOF extends Instruction {

    // The type to check against
    String type;

    /**
     * Creates a new INSTANCEOF instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param type the type to check against
     */
    public INSTANCEOF(int iid, int mid, String type) {
        super(iid, mid);
        this.type = type;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitINSTANCEOF(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("INSTANCEOF " + type);
    }
}
