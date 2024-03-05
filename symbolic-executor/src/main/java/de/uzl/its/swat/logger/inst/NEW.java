package de.uzl.its.swat.logger.inst;

/**
 * NEW - Create new object.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.new">Java VM specification</a>.
 */
public class NEW extends Instruction {

    // The type of the object to be created
    String type;

    // The class index
    public int cIdx;


    /**
     * Creates a new NEW instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param type the type of the object to be created
     * @param cIdx the class index
     */
    public NEW(int iid, int mid, String type, int cIdx) {
        super(iid, mid);
        this.type = type;
        this.cIdx = cIdx;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitNEW(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("NEW " + type + " " + cIdx);
    }
}