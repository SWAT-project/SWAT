package de.uzl.its.swat.symbolic.instruction;

/**
 * MULTIANEWARRAY - Create new multidimensional array. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.multianewarray">Java
 * VM specification</a>.
 */
public class MULTIANEWARRAY extends Instruction {

    // The descriptor of the array
    public String desc;

    // The number of dimensions
    public int dims;

    /**
     * Creates a new MULTIANEWARRAY instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param desc the descriptor of the array
     * @param dims the number of dimensions
     */
    public MULTIANEWARRAY(int iid, int mid, String desc, int dims) {
        super(iid, mid);
        this.desc = desc;
        this.dims = dims;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitMULTIANEWARRAY(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("MULTIANEWARRAY " + desc + " " + dims);
    }
}
