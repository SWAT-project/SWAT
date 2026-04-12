package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

public class GET_FIELD_REFLECTION extends InvokeIdInstruction {

    public String reflectFieldName;
    public String reflectObjectOwner;

    public String owner;
    public String name;
    public String desc;

    public int modifiers;

    public GET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, int modifiers, long iid, long invokeId) {
        super(iid, invokeId);

        this.reflectFieldName = reflectFieldName;
        this.reflectObjectOwner = reflectObjectOwner;

        this.owner = owner;
        this.name = name;
        this.desc = desc;

        this.modifiers = modifiers;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitGET_FIELD_REFLECTION(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {

        // return genericToString("CLINIT " + type + " " + cIdx);
        return genericToString("GET_FIELD_REFLECTION " + " ( " + iid + ", " + reflectObjectOwner + ", " + reflectFieldName + " ), InvokeId: " + invokeId);
    }
}