package de.uzl.its.swat.logger.inst;

/**
 * NEWARRAY - Create new array.
 * For more information see the  <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5.newarray">Java VM specification</a>.
 */
public class NEWARRAY extends Instruction {

    // The type of the array to be created
    public int atype;


    /**
     * Creates a new NEWARRAY instruction.
     * @param iid instruction id.
     * @param mid method id.
     * @param atype the type of the array to be created
     */
    public NEWARRAY(int iid, int mid, int atype) {
        super(iid, mid);
        this.atype = atype;
    }

    /**
     * Accept method for the visitor.
     * @param visitor the visitor
     */
    public void visit(IVisitor visitor) {
        visitor.visitNEWARRAY(this);
    }

    @Override
    public String toString() {
        String type = "";
        switch (atype) {
            case 4 -> type = "T_BOOLEAN";
            case 5 -> type = "T_CHAR";
            case 6 -> type = "T_FLOAT";
            case 7 -> type = "T_DOUBLE";
            case 8 -> type = "T_BYTE";
            case 9 -> type = "T_SHORT";
            case 10 -> type = "T_INT";
            case 11 -> type = "T_LONG";
        }
        return genericToString("NEWARRAY " + type);
    }
}
