package de.uzl.its.swat.symbolic.instruction;

/**
 * INVOKEDYNAMIC - Invoke a dynamically-computed call site. For more information see the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.invokedynamic">Java
 * VM specification</a>.
 */
public class INVOKEDYNAMIC extends Instruction {
    // The owner of the method
    public String owner;
    // The name of the method
    public String name;
    // The descriptor of the method
    public String desc;
    // Additional information for lambda expressions
    public String lambda;

    /**
     * Creates a new INVOKEDYNAMIC instruction.
     *
     * @param iid instruction id.
     * @param mid method id.
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     * @param lambda additional information for lambda expressions
     */
    public INVOKEDYNAMIC(int iid, int mid, String owner, String name, String desc, String lambda) {
        super(iid, mid);
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.lambda = lambda;
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void accept(IVisitor visitor) {
        visitor.visitINVOKEDYNAMIC(this);
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("INVOKEDYNAMIC " + owner + "/" + name + ":" + desc + " " + lambda);
    }
}
