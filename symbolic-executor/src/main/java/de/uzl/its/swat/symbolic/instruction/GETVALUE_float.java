package de.uzl.its.swat.symbolic.instruction;

/**
 * GETVALUE_float - Custom method call handled as an instruction to fetch the concrete value that
 * was produced/ loaded by another instruction.
 */
public class GETVALUE_float extends GETVALUE_primitive {

    /**
     * Creates a new value fetch instruction for floats.
     *
     * @param v the concrete value
     * @param i an identifier
     */
    public GETVALUE_float(float v, int i) {
        super(v, i);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitGETVALUE_float(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("F");
    }
}
