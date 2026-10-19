package de.uzl.its.swat.symbolic.value.reference.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

/**
 * Java's case mappings as formulas: the ignore-case comparison against a concrete string as a
 * regular expression of the chars' case classes (tabulated from the running JVM), and the case
 * mappings and ignore-case comparisons of ASCII strings char by char.
 *
 * <p>Beyond ASCII the mappings are tables of a few hundred ranges; unrolled over a string, Z3 does
 * not decide queries over them (even simple ones time out), so a model separates the ASCII strings
 * by a branch and leaves the others unmodelled.
 */
public final class CaseMapping {

    public enum Kind {
        /** Character.toLowerCase / String.toLowerCase */
        LOWER,
        /** Character.toUpperCase / String.toUpperCase */
        UPPER
    }

    /** The chars by their key Character.toLowerCase(Character.toUpperCase(c)), by which the ignore-case comparisons compare. */
    private static final class FoldClasses {
        static final Map<Integer, List<Integer>> CLASSES = new HashMap<>();

        static {
            for (int c = 0; c <= 0xFFFF; c++) {
                CLASSES.computeIfAbsent(fold(c), k -> new ArrayList<>()).add(c);
            }
        }
    }

    private final BooleanFormulaManager bmgr;
    private final IntegerFormulaManager imgr;
    private final StringFormulaManager smgr;

    public CaseMapping(FormulaManager fmgr) {
        this.bmgr = fmgr.getBooleanFormulaManager();
        this.imgr = fmgr.getIntegerFormulaManager();
        this.smgr = fmgr.getStringFormulaManager();
    }

    /** The key two chars are compared by when case is ignored. */
    static int fold(int c) {
        return Character.toLowerCase(Character.toUpperCase(c));
    }

    /**
     * Whether String.toLowerCase/toUpperCase with this locale maps ASCII letters to ASCII letters:
     * Java has special rules only for Turkish, Azerbaijani ('I' to dotless 'ı') and Lithuanian.
     */
    public static boolean rootRules(Locale locale) {
        String lang = locale.getLanguage();
        return !lang.equals("tr") && !lang.equals("az") && !lang.equals("lt");
    }

    /** The chars of a Java string, one SMT char per UTF-16 char (makeString would join surrogate pairs). */
    public StringFormula chars(String s) {
        StringFormula f = smgr.makeString("");
        for (char c : s.toCharArray()) {
            f = smgr.concat(f, smgr.fromCodePoint(imgr.makeNumber(c)));
        }
        return f;
    }

    /**
     * The strings equal to t ignoring case (t has no surrogates): at each position, the chars with
     * the same key as t's char there. A surrogate of the other string matches none of them, also
     * as part of a pair (Java compares the pair's code point then, whose key is not one of t's).
     */
    public RegexFormula equalIgnoringCase(String t) {
        RegexFormula regex = smgr.makeRegex("");
        for (char c : t.toCharArray()) {
            RegexFormula chars = null;
            for (int other : FoldClasses.CLASSES.get(fold(c))) {
                RegexFormula one = smgr.range((char) other, (char) other);
                chars = chars == null ? one : smgr.union(chars, one);
            }
            regex = smgr.concat(regex, chars);
        }
        return regex;
    }

    /** Whether all chars of s are ASCII (below 128). */
    public BooleanFormula isAscii(StringFormula s) {
        return smgr.in(s, smgr.closure(smgr.range('\u0000', '\u007f')));
    }

    /** The code of the char of s at index i */
    public IntegerFormula codeAt(StringFormula s, IntegerFormula i) {
        return smgr.toCodePoint(smgr.charAt(s, i));
    }

    private BooleanFormula between(IntegerFormula c, char lo, char hi) {
        return bmgr.and(imgr.greaterOrEquals(c, imgr.makeNumber(lo)), imgr.lessOrEquals(c, imgr.makeNumber(hi)));
    }

    /** The key of an ASCII char: the lower case of a letter, else the char itself. */
    public IntegerFormula foldAscii(IntegerFormula c) {
        return bmgr.ifThenElse(between(c, 'A', 'Z'), imgr.add(c, imgr.makeNumber('a' - 'A')), c);
    }

    /** toLowerCase/toUpperCase of the first n chars of s, all ASCII (for a locale with the root rules). */
    public StringFormula mapAscii(Kind kind, StringFormula s, int n) {
        StringFormula result = smgr.makeString("");
        for (int i = 0; i < n; i++) {
            StringFormula ch = smgr.charAt(s, imgr.makeNumber(i));
            IntegerFormula c = smgr.toCodePoint(ch);
            StringFormula mapped = kind == Kind.LOWER
                    ? bmgr.ifThenElse(between(c, 'A', 'Z'), smgr.fromCodePoint(imgr.add(c, imgr.makeNumber('a' - 'A'))), ch)
                    : bmgr.ifThenElse(between(c, 'a', 'z'), smgr.fromCodePoint(imgr.subtract(c, imgr.makeNumber('a' - 'A'))), ch);
            result = smgr.concat(result, mapped);
        }
        return result;
    }

    /** The result of an ignore-case comparison: whether no char differed, and the int compareToIgnoreCase returns. */
    public record Comparison(BooleanFormula noDifference, IntegerFormula value) {}

    /**
     * Java's ignore-case comparison of a[aStart, aStart + tlen) with b[bStart, bStart + olen), both
     * ASCII, char by char over the concrete lengths: the difference of the keys of the first chars
     * that differ, else tlen - olen.
     */
    public Comparison compareAsciiIgnoringCase(StringFormula a, IntegerFormula aStart, int tlen,
                                               StringFormula b, IntegerFormula bStart, int olen) {
        int n = Math.min(tlen, olen);
        List<BooleanFormula> equal = new ArrayList<>();
        IntegerFormula value = imgr.makeNumber(tlen - olen);
        IntegerFormula[] diff = new IntegerFormula[n];
        BooleanFormula[] same = new BooleanFormula[n];
        for (int k = 0; k < n; k++) {
            IntegerFormula x = foldAscii(codeAt(a, imgr.add(aStart, imgr.makeNumber(k))));
            IntegerFormula y = foldAscii(codeAt(b, imgr.add(bStart, imgr.makeNumber(k))));
            same[k] = imgr.equal(x, y);
            diff[k] = imgr.subtract(x, y);
            equal.add(same[k]);
        }
        for (int k = n - 1; k >= 0; k--) value = bmgr.ifThenElse(same[k], value, diff[k]);
        return new Comparison(bmgr.and(equal), value);
    }
}
