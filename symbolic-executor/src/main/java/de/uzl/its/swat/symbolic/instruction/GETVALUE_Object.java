package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * GETVALUE_Object - Custom method call handled as an instruction to fetch the concrete value that
 * was produced/ loaded by another instruction.
 *
 * @param <T> the type of the concrete object
 */
public class GETVALUE_Object<T> extends Instruction {

    // Address (?) of the value
    public int address;

    // Identifier
    public int i;

    // The object to be fetched
    public T val;

    /**
     * Creates a new value fetch instruction for objects.
     *
     * @param address the address of the value
     * @param val the concrete value
     * @param i an identifier
     */
    public GETVALUE_Object(long iid, int address, T val, int i) {
        super(iid);
        this.address = address;
        this.val = val;
        this.i = i;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitGETVALUE_Object(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        if (val == null) {
            return "[VALUE FETCH] "
                    + "Ljava/lang/Object; @"
                    + Integer.toHexString(address)
                    + " ("
                    + i
                    + ")";
        }
        // It is important not to print all values as this could cause execution of instrumented code. Hence only Java methods allowed.
        String valStr = ""; // (val instanceof String) ? val.toString() : "";
        if(val instanceof String) valStr = val.toString();
        else if(val instanceof Integer) valStr = Integer.toString((Integer) val);
        else if(val instanceof Long) valStr = Long.toString((Long) val);
        else if(val instanceof Double) valStr = Double.toString((Double) val);
        else if(val instanceof Float) valStr = Float.toString((Float) val);
        else if(val instanceof Boolean) valStr = Boolean.toString((Boolean) val);
        else if(val instanceof Character) valStr = Character.toString((Character) val);

        return "[VALUE FETCH] "
                + "L"
                + val.getClass()
                + "; @"
                + Integer.toHexString(address)
                + " "
                + valStr
                + " ("
                + i
                + ")" + " (id: " + iid + ")";
    }
}
