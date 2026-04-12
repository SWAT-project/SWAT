package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * NEW - Create new object. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.new">Java VM
 * specification</a>.
 */
public class CLINIT extends InvokeIdInstruction {

    // The type of the object to be created
    // String type;

    // The class index
    public int cIdx;
    public final String desc = "()V";
    public final String name = "<clinit>";

    /**
     * Creates a new CLINIT helper instruction marking the beginning of a static initializer
     *
     * @param iid instruction id.
     * @param cIdx the class index
     * @param invokeId the invoke id
     */
    // public CLINIT(long iid, long mid, String type, int cIdx) {
    public CLINIT(long iid, int cIdx, long invokeId) {
        super(iid, invokeId);
        // this.type = type;
        this.cIdx = cIdx;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitCLINIT(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {

        // return genericToString("CLINIT " + type + " " + cIdx);
        return genericToString("CLINIT " + " " + cIdx);
    }
}
