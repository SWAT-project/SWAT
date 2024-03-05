package de.uzl.its.swat.logger.inst;

/**
 * INVOKESTATIC - Invoke a class (static) method.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.invokestatic">Java VM specification</a>.
 */
public class INVOKESTATIC extends Instruction {

    // The owner of the method
    public String owner;
    // The name of the method
    public String name;
    // The descriptor of the method
    public String desc;

    /**
     * Creates a new INVOKESPECIAL instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     */
    public INVOKESTATIC(int iid, int mid, String owner, String name, String desc) {
        super(iid, mid);
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitINVOKESTATIC(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("INVOKESTATIC " + owner + "/" + name + ":" + desc);
    }
}

