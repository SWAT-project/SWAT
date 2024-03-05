package de.uzl.its.swat.logger.inst;

/**
 * GETVALUE_Object - Custom method call handled as an instruction to fetch the concrete value that was produced/ loaded by another instruction.
 * @param <T> the type of the concrete object
 */
public class GETVALUE_Object<T> extends Instruction {

    // Address (?) of the value
    public int v;

    // Identifier
    public int i;

    // The object to be fetched
    public T val;

    /**
     * Creates a new value fetch instruction for objects.
     * @param v the address of the value
     * @param val the concrete value
     * @param i an identifier
     */
    public GETVALUE_Object(int v, T val, int i) {
        super(-1, -1);
        this.v = v;
        this.val = val;
        this.i = i;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitGETVALUE_Object(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        if (val == null) {
            return "[VALUE FETCH] " + "Ljava/lang/Object; @" + Integer.toHexString(v) + " (" + i + ")";
        }
        return "[VALUE FETCH] " + "L" + val.getClass() + "; @" + Integer.toHexString(v) + " " + val + " (" + i + ")";
    }
}
