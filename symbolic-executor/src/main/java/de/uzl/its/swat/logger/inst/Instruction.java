package de.uzl.its.swat.logger.inst;

import java.io.Serializable;

/**
 * Base class for all instructions. Should be implemented by each instruction
 */
public abstract class Instruction implements Serializable {
    // Unique instruction id, -1 if unused
    public int iid;
    // Unique method id, -1 if unused
    public int mid;

    /**
     * Abstract accept method for the visitor.
     * @param visitor the visitor
     */
    public abstract void visit(IVisitor visitor);

    /**
     * Creates a new instruction.
     * @param iid instruction id.
     * @param mid method id.
     */
    public Instruction(int iid, int mid) {
        this.iid = iid;
        this.mid = mid;
    }

    /**
     * Returns the string representation of the instruction.
     * @param prefix Instruction specific information
     * @return the representation.
     */
    public String genericToString(String prefix) {
        String s = prefix;
        if (iid != -1){
            s += " (" + iid;
            if (mid != -1){
                s += ", " + mid;
            }
            s += ")";
        }
        return s;
    }
}
