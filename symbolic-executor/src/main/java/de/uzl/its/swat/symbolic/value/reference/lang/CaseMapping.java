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
 * Java's ignore-case comparisons as formulas: against a concrete string as a regular expression
 * of the chars' case classes (tabulated from the running JVM), and of two ASCII strings char by
 * char.
 *
 * <p>Beyond ASCII the case mappings are tables of a few hundred ranges; unrolled over a string, Z3
 * does not decide queries over them (even simple ones time out), so a model separates the ASCII
 * strings by a branch and leaves the others unmodelled.
 */
public final class CaseMapping {

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

    /**
     * toLowerCase/toUpperCase of an ASCII string of length n, char by char: a letter of the other
     * case shifted by 32, every other char kept. For ASCII strings this is Java's case mapping
     * in every locale with the root rules ({@link #rootRules}), and it keeps the length.
     */
    public StringFormula asciiCase(StringFormula s, int n, boolean lower) {
        char lo = lower ? 'A' : 'a';
        char hi = lower ? 'Z' : 'z';
        IntegerFormula shift = imgr.makeNumber(lower ? 'a' - 'A' : 'A' - 'a');
        StringFormula result = smgr.makeString("");
        for (int i = 0; i < n; i++) {
            IntegerFormula c = codeAt(s, imgr.makeNumber(i));
            result = smgr.concat(result, smgr.fromCodePoint(bmgr.ifThenElse(between(c, lo, hi), imgr.add(c, shift), c)));
        }
        return result;
    }

    /**
     * The case mappings of single chars, tabulated from the running JVM with the root locale (the
     * same as every locale with the root rules): for each char t, the BMP chars (no surrogates)
     * whose lower/upper case is exactly t; and the chars that may come out of a mapping that is
     * not one char to one char: of a multi-char expansion (e.g. "İ" to "i̇", "ß"
     * to "SS"), of a supplementary code point, or of a context-dependent one (Greek capital sigma
     * to final or non-final sigma).
     */
    private static final class Tables {
        static final Map<Character, List<Character>> LOWER = new HashMap<>();
        static final Map<Character, List<Character>> UPPER = new HashMap<>();
        static final java.util.Set<Character> LOWER_UNSAFE = new java.util.HashSet<>();
        static final java.util.Set<Character> UPPER_UNSAFE = new java.util.HashSet<>();

        static {
            for (int c = 0; c <= 0xFFFF; c++) {
                if (Character.isSurrogate((char) c)) continue;
                add(String.valueOf((char) c), (char) c, LOWER, LOWER_UNSAFE, true);
                add(String.valueOf((char) c), (char) c, UPPER, UPPER_UNSAFE, false);
            }
            for (int cp = Character.MIN_SUPPLEMENTARY_CODE_POINT; cp <= Character.MAX_CODE_POINT; cp++) {
                if (!Character.isDefined(cp)) continue;
                String s = new String(Character.toChars(cp));
                for (char r : s.toLowerCase(Locale.ROOT).toCharArray()) LOWER_UNSAFE.add(r);
                for (char r : s.toUpperCase(Locale.ROOT).toCharArray()) UPPER_UNSAFE.add(r);
            }
            // Capital sigma lower-cases to final or non-final sigma, by its context
            LOWER_UNSAFE.add('ς');
            LOWER_UNSAFE.add('σ');
        }

        private static void add(String s, char c, Map<Character, List<Character>> table, java.util.Set<Character> unsafe, boolean lower) {
            String r = lower ? s.toLowerCase(Locale.ROOT) : s.toUpperCase(Locale.ROOT);
            if (r.length() == 1) {
                table.computeIfAbsent(r.charAt(0), k -> new ArrayList<>()).add(c);
            } else {
                for (char x : r.toCharArray()) unsafe.add(x);
            }
        }
    }

    /**
     * Whether a comparison of the lower/upper case of a string with the concrete t is modelled as
     * a regular expression over the string ({@link #preimage}): t has no char a mapping that is
     * not one char to one char may produce. Then each occurrence of t in the case of s comes from
     * |t| consecutive chars of s, each mapped on its own.
     */
    public static boolean byPreimage(String t, boolean lower) {
        java.util.Set<Character> unsafe = lower ? Tables.LOWER_UNSAFE : Tables.UPPER_UNSAFE;
        for (char c : t.toCharArray()) {
            if (Character.isSurrogate(c) || unsafe.contains(c)) return false;
        }
        return true;
    }

    /**
     * The strings whose lower/upper case is t, if {@link #byPreimage}(t): at each position the
     * chars that map to t's char there.
     */
    public RegexFormula preimage(String t, boolean lower) {
        Map<Character, List<Character>> table = lower ? Tables.LOWER : Tables.UPPER;
        RegexFormula regex = smgr.makeRegex("");
        for (char c : t.toCharArray()) {
            RegexFormula chars = null;
            for (char other : table.getOrDefault(c, List.of())) {
                RegexFormula one = smgr.range(other, other);
                chars = chars == null ? one : smgr.union(chars, one);
            }
            if (chars == null) return smgr.none();
            regex = smgr.concat(regex, chars);
        }
        return regex;
    }

    /** The longest string whose case mapping is modelled char by char (its length is pinned). */
    public static final int MAX_PINNED = 32;

    /**
     * A case mapping call of a symbolic string whose library branches (StringCallBranches) were
     * recorded: they pin the length to n and split the ASCII strings from the others. The model's
     * result is the function {@code function} of the string; the branches matter only where a
     * branch constraint mentions it (DTOBuilder flags them merged otherwise). {@code exact}: the
     * string is ASCII, so the model is exact on the branches' side.
     */
    public record Call(StringValue receiver, String function, int n, boolean exact) {}

    /** Numbers the case mapping calls of this execution, so that every call has its own function. */
    private static final java.util.concurrent.atomic.AtomicInteger CALLS = new java.util.concurrent.atomic.AtomicInteger();

    /** The call whose branches were recorded last on this thread, until its model takes it. */
    private static final ThreadLocal<Call> PENDING = new ThreadLocal<>();

    /**
     * A new function name for a case mapping model: exact ones are model functions, the others
     * approximate (the explorer recognises those by SampledMath's prefix {@code math_}).
     */
    public static String newFunction(boolean lower, boolean exact) {
        return (exact ? "case_" : "math_case_") + (lower ? "lower_" : "upper_") + CALLS.getAndIncrement();
    }

    /** Called when the branches of a case mapping call are recorded (null: none were). */
    public static void setPending(Call call) {
        PENDING.set(call);
    }

    /** The recorded call of this receiver, if its branches were recorded, else null; it is taken. */
    public static Call takePending(StringValue receiver) {
        Call call = PENDING.get();
        PENDING.remove();
        return call != null && call.receiver() == receiver ? call : null;
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
