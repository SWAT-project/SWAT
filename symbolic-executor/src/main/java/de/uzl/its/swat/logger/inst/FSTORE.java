package de.uzl.its.swat.logger.inst;

/**
 * FSTORE - Store float into local variable.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.fstore">Java VM specification</a>.
 */
public class FSTORE extends Instruction {
    // local variable index to store into
    public int var;

    /**
     * Creates a new FSTORE instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param var local variable index to store into
     */
    public FSTORE(int iid, int mid, int var) {
        super(iid, mid);
        this.var = var;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitFSTORE(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("FSTORE " + var);
    }
}