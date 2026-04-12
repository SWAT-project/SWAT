package de.uzl.its.swat.symbolic.instruction;

import de.uzl.its.swat.common.exceptions.SymbolicInstructionException;

import java.io.Serializable;

/** Base class for all instructions. Should be implemented by each instruction */
public abstract class Instruction implements Serializable {
    // Unique instruction id
    public long iid;

    /**
     * Abstract accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public abstract void accept(IVisitor visitor) throws SymbolicInstructionException;

    /**
     * Creates a new instruction.
     *
     * @param iid instruction id.
     */
    public Instruction(long iid) {
        this.iid = iid;
    }

    /**
     * Returns the string representation of the instruction.
     *
     * @param prefix Instruction specific information
     * @return the representation.
     */
    public String genericToString(String prefix) {
        String s = prefix;
        if (iid != -1) {
            s += " (id: " + iid + ")";
        }
        return s;
    }
}
