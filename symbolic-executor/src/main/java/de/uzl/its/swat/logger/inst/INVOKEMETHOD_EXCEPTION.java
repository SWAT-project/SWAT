package de.uzl.its.swat.logger.inst;

/**
 * INVOKEMETHOD_EXCEPTION - Custom method handled as an instruction to signalize a method end. Used
 * to detect exceptions.
 */
public class INVOKEMETHOD_EXCEPTION extends Instruction {

    public INVOKEMETHOD_EXCEPTION() {
        super(-1, -1);
    }

    /**
     * Accept method for the visitor.
     *
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitINVOKEMETHOD_EXCEPTION(this);
    }

    @Override
    public String toString() {
        return "[METHOD EXCEPTION]";
    }
}
