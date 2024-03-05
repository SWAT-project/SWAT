package de.uzl.its.swat.logger.inst;

/**
 * GETVALUE_double - Custom method call handled as an instruction to fetch the concrete value that
 * was produced/ loaded by another instruction.
 */
public class GETVALUE_double extends GETVALUE_primitive {

    /**
     * Creates a new value fetch instruction for doubles.
     *
     * @param v the concrete value
     * @param i an identifier
     */
    public GETVALUE_double(double v, int i) {
        super(v, i);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_double(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("D");
    }
}
