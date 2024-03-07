package de.uzl.its.swat.symbolic.instruction;

/**
 * LDC(_String) - Push item from run-time constant pool. LDC instructions are handled based on the
 * datatype to load. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.lconst_l">Java VM
 * specification</a>.
 */
public class LDC_String extends Instruction {

    // The value to load
    public String c;

    // The address of the object
    public int address;

    /**
     * Creates a new LDC_String instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param c the value to load
     * @param address the address of the object
     */
    public LDC_String(int iid, int mid, String c, int address) {
        super(iid, mid);
        this.c = c;
        this.address = address;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitLDC_String(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString(
                "LDC [Ljava/lang/String; @" + Integer.toHexString(address) + "] " + c);
    }
}
