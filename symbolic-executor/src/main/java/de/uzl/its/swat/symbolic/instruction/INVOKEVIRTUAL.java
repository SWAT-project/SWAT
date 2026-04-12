package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * INVOKEVIRTUAL - Invoke instance method; dispatch based on class. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.invokevirtual">Java
 * VM specification</a>.
 */
public class INVOKEVIRTUAL extends InvokeIdInstruction {

    // The owner of the method
    public String owner;
    // The name of the method
    public String name;
    // The descriptor of the method
    public String desc;

    /**
     * Creates a new INVOKESPECIAL instruction.
     *
     * @param iid instruction id.
     * @param invokeId invoke id.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    public INVOKEVIRTUAL(
            long iid, long invokeId, String owner, String name, String desc) {
        super(iid, invokeId);
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) throws SymbolicInstructionException {
        visitor.visitINVOKEVIRTUAL(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("INVOKEVIRTUAL " + owner + "/" + name + ":" + desc);
    }
}
