package de.uzl.its.swat.common.exceptions;

import de.uzl.its.swat.symbolic.instruction.Instruction;

public class SymbolicInstructionException extends Exception{
    public SymbolicInstructionException(Instruction inst, String message) {
        super("Error during symbolic execution of instruction " + inst + " --> " + message);
    }
    public SymbolicInstructionException(Instruction inst, Throwable t) {
        super("Error during symbolic execution of instruction " + inst, t);
    }

}
