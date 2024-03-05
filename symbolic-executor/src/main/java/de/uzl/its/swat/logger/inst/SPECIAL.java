package de.uzl.its.swat.logger.inst;

/**
 * A special probe instruction added by the instrumentation. The int value identifies which branch
 * does the instruction correspond to.
 */
public class SPECIAL extends Instruction {

    // Marker used in symbolic interpreter for example for branch detection
    public int i;


    /**
     * Creates a new SPECIAL instruction.
     * @param i marker value
     */
    public SPECIAL(int i) {
        super(-1, -1);
        this.i = i;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitSPECIAL(this);
    }

    /**
     * Returns the string representation of the instruction.
     * @return the representation.
     */
    @Override
    public String toString() {
        return genericToString("[SPECIAL] " + i);
    }
}