package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

/**
 * INVOKESPECIAL - Invoke instance method; direct invocation of instance initialization methods and
 * methods of the current class and its supertypes. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.invokespecial">Java
 * VM specification</a>.
 */
public class INVOKESPECIAL extends InvokeIdInstruction {

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
     * @param invokeId invoke id
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    public INVOKESPECIAL(
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
        visitor.visitINVOKESPECIAL(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("INVOKESPECIAL " + owner + "/" + name + ":" + desc);
    }
}
