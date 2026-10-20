package de.uzl.its.swat.symbolic.value.reference.lang;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

/** The UTF-16 chars of a Java string as formulas: their codes, surrogates and surrogate pairs. */
public final class Utf16 {
    private final BooleanFormulaManager bmgr;
    private final IntegerFormulaManager imgr;
    private final StringFormulaManager smgr;

    public Utf16(FormulaManager fmgr) {
        this.bmgr = fmgr.getBooleanFormulaManager();
        this.imgr = fmgr.getIntegerFormulaManager();
        this.smgr = fmgr.getStringFormulaManager();
    }

    /** The code of the char of s at index i */
    public IntegerFormula codeAt(StringFormula s, IntegerFormula i) {
        return smgr.toCodePoint(smgr.charAt(s, i));
    }

    public BooleanFormula isHighSurrogate(IntegerFormula c) {
        return bmgr.and(imgr.greaterOrEquals(c, imgr.makeNumber(Character.MIN_HIGH_SURROGATE)),
                imgr.lessOrEquals(c, imgr.makeNumber(Character.MAX_HIGH_SURROGATE)));
    }

    public BooleanFormula isLowSurrogate(IntegerFormula c) {
        return bmgr.and(imgr.greaterOrEquals(c, imgr.makeNumber(Character.MIN_LOW_SURROGATE)),
                imgr.lessOrEquals(c, imgr.makeNumber(Character.MAX_LOW_SURROGATE)));
    }

    /** Character.toCodePoint(high, low) */
    public IntegerFormula toCodePoint(IntegerFormula high, IntegerFormula low) {
        return imgr.add(imgr.multiply(imgr.subtract(high, imgr.makeNumber(Character.MIN_HIGH_SURROGATE)), imgr.makeNumber(1024)),
                imgr.add(imgr.subtract(low, imgr.makeNumber(Character.MIN_LOW_SURROGATE)),
                        imgr.makeNumber(Character.MIN_SUPPLEMENTARY_CODE_POINT)));
    }

    /** The chars of a Java string, one SMT char per UTF-16 char (makeString would join surrogate pairs). */
    public StringFormula chars(String s) {
        StringFormula f = smgr.makeString("");
        for (char c : s.toCharArray()) {
            f = smgr.concat(f, smgr.fromCodePoint(imgr.makeNumber(c)));
        }
        return f;
    }
}
