package de.uzl.its.swat.logger.inst;

/**
 * INVOKEMETHOD_END - Custom method handled as an instruction to signalize a method end.
 * Signalizes a method end.
 */
public class INVOKEMETHOD_END extends Instruction {

    /**
     * Creates a new INVOKEMETHOD_END instruction.
     */
    public INVOKEMETHOD_END() {
        super(-1, -1);
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitINVOKEMETHOD_END(this);
    }

    @Override
    public String toString() {
        return "[METHOD END]";
    }
}
