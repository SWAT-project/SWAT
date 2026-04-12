package de.uzl.its.swat.symbolic.instruction;

public abstract class InvokeIdInstruction extends Instruction {

    // InvokeID matching an Invoke with the MethodEnd / Exception
    public long invokeId;

    public InvokeIdInstruction(long iid, long invokeId) {
        super(iid);
        this.invokeId = invokeId;
    }

    /**
     * Returns the string representation of the invoke instruction.
     *
     * @param prefix Instruction specific information
     * @return the representation.
     */
    public String genericToString(String prefix) {
        String s = prefix;
        if (iid != -1) {
            s += " (id: " + iid;
            s += ", invokeId: " + invokeId + ")";
        }
        return s;
    }
}
