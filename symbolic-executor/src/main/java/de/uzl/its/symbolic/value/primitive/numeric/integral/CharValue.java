package de.uzl.its.symbolic.value.primitive.numeric.integral;

import de.uzl.its.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.symbolic.value.reference.lang.StringValue;
import java.util.ArrayList;
import org.sosy_lab.java_smt.api.*;

public class CharValue extends NumericalValue<NumeralFormula.IntegerFormula, Character> {
    private static final String symbolicPrefix = "C";

    public CharValue(SolverContext context, char concrete) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = imgr.makeNumber(concrete);
    }

    public CharValue(SolverContext context, char concrete, StringFormula formula) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = stringToInt(formula);
    }

    public CharValue(SolverContext context, char concrete, NumeralFormula.IntegerFormula formula) {
        this.context = context;
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Creates a formula that asserts that this symbolic value is withing the bounds of this type
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        return upper
                ? imgr.lessOrEquals(imgr.makeVariable(name), imgr.makeNumber(Character.MAX_VALUE))
                : imgr.greaterOrEquals(
                        imgr.makeVariable(name), imgr.makeNumber(Character.MIN_VALUE));
    }
    /**
     * Turns this CharValue into a symbolic variable
     *
     * @param namePrefix
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(String namePrefix) {
        initSymbolic(namePrefix);
        formula = imgr.makeVariable(name);
        return name;
    }

    /**
     * Turns this CharValue into a symbolic variable
     *
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC() {
        initSymbolic(symbolicPrefix);
        formula = imgr.makeVariable(name);
        return name;
    }

    /**
     * Turns this IntValue into a symbolic variable
     *
     * @param idx
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(long idx) {
        initSymbolic(symbolicPrefix, idx);
        formula = imgr.makeVariable(name);
        return name;
    }

    private NumeralFormula.IntegerFormula stringToInt(StringFormula s) {
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        int max = 0x7F;
        int segmentSize = 0x7F;
        ArrayList<StringFormula> chars = new ArrayList<>();
        for (int i = 0; i < (max / segmentSize); i++) {
            chars.add(smgr.makeString(getASCIIchars(i * segmentSize, (i + 1) * segmentSize)));
        }
        return tmp(chars, s);
    }

    public NumeralFormula.IntegerFormula tmp(ArrayList<StringFormula> chars, StringFormula s) {
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        StringFormula c = chars.get(0);
        chars.remove(0);
        NumeralFormula.IntegerFormula zero = imgr.makeNumber(0);
        if (chars.size() > 0) {
            return bmgr.ifThenElse(smgr.contains(s, c), smgr.indexOf(c, s, zero), tmp(chars, s));
        } else {
            return smgr.indexOf(c, s, zero);
        }
    }

    public static String getASCIIchars(int begin, int end) {
        StringBuilder buf = new StringBuilder();
        for (int codePoint = begin; codePoint <= end; ++codePoint) {
            buf.append(String.format("\\u{%x}", codePoint));
        }
        return buf.toString();
    }

    @Override
    public CharValue asCharValue() {
        return this;
    }

    @Override
    public IntValue asIntValue() {
        return new IntValue(context, concrete, formula);
    }

    @Override
    public StringValue asStringValue() {
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        return new StringValue(
                context,
                String.valueOf(concrete),
                smgr.charAt(smgr.makeString(CharValue.getASCIIchars(0, 591)), formula),
                -1);
    }

    @Override
    public String getConcreteEncoded() {
        return Character.toString(concrete);
    }

    /**
     * Returns the string representation of the value used to visualize the stack. The representation is not complete.
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("C");
    }
}
