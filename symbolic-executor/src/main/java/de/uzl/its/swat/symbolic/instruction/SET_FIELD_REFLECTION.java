package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

public class SET_FIELD_REFLECTION extends InvokeIdInstruction {

    public String reflectFieldName;
    public String reflectObjectOwner;
    public boolean isWideOperand;

    public String owner;
    public String name;
    public String desc;

    public int modifiers;

    public SET_FIELD_REFLECTION(String owner, String name, String desc, String reflectFieldName, String reflectObjectOwner, boolean isWideOperand, int modifiers, long iid, long invokeId) {
        super(iid, invokeId);

        this.owner = owner;
        this.name = name;
        this.desc = desc;

        this.reflectFieldName = reflectFieldName;
        this.reflectObjectOwner = reflectObjectOwner;
        this.isWideOperand = isWideOperand;

        this.modifiers = modifiers;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitSET_FIELD_REFLECTION(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("SET_FIELD_REFLECTION " + " ( " + iid + ", " + reflectObjectOwner + ", " + reflectFieldName + " ), InvokeId: " + invokeId);
    }
}
