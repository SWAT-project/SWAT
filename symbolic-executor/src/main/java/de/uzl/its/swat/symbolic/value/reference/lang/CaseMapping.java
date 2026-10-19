package de.uzl.its.swat.symbolic.value.reference.lang;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

/**
 * Java's case mappings, tabulated from the running JVM (java.lang.Character) and encoded as
 * formulas over a code (an Int): the code point mappings, the mapping of one char of a String by
 * {@code toLowerCase}/{@code toUpperCase} (for a locale other than tr, az and lt), and the key the
 * ignore-case comparisons compare by.
 */
public final class CaseMapping {

    public enum Kind {
        /** Character.toLowerCase(int) */
        LOWER,
        /** Character.toUpperCase(int) */
        UPPER,
        /** Character.toLowerCase(Character.toUpperCase(int)): two chars are equal ignoring case iff this is */
        FOLD
    }

    /** The codes lo, lo + stride, ..., hi, which all map to code + delta. */
    private record Segment(int lo, int hi, int stride, int delta) {}

    private static final class Tables {
        static final Map<Kind, List<Segment>> SEGMENTS = new EnumMap<>(Kind.class);

        /**
         * The chars (no surrogates) that String.toLowerCase/toUpperCase maps to other than the one
         * char the code point mapping gives, e.g. 'ß' to "SS".
         */
        static final Map<Kind, TreeMap<Integer, String>> SPECIALS = new EnumMap<>(Kind.class);

        /** The chars by their FOLD key. */
        static final Map<Integer, List<Integer>> FOLD_CLASSES = new HashMap<>();

        static {
            for (Kind kind : Kind.values()) {
                List<Segment> segments = new ArrayList<>();
                for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
                    int delta = map(kind, cp) - cp;
                    if (delta == 0) continue;
                    Segment last = segments.isEmpty() ? null : segments.get(segments.size() - 1);
                    if (last != null && last.delta() == delta
                            && (cp - last.hi() == last.stride() || (last.lo() == last.hi() && cp - last.hi() <= 2))) {
                        segments.set(segments.size() - 1, new Segment(last.lo(), cp, cp - last.hi(), delta));
                    } else {
                        segments.add(new Segment(cp, cp, 1, delta));
                    }
                }
                SEGMENTS.put(kind, segments);
            }
            for (Kind kind : new Kind[] {Kind.LOWER, Kind.UPPER}) {
                TreeMap<Integer, String> specials = new TreeMap<>();
                for (int c = 0; c <= 0xFFFF; c++) {
                    if (Character.isSurrogate((char) c)) continue;
                    String chars = mapString(kind, String.valueOf((char) c));
                    if (chars.length() != 1 || chars.charAt(0) != map(kind, c)) specials.put(c, chars);
                }
                SPECIALS.put(kind, specials);
            }
            for (int c = 0; c <= 0xFFFF; c++) {
                FOLD_CLASSES.computeIfAbsent(map(Kind.FOLD, c), k -> new ArrayList<>()).add(c);
            }
        }
    }

    private final FormulaManager fmgr;
    private final BooleanFormulaManager bmgr;
    private final IntegerFormulaManager imgr;
    private final StringFormulaManager smgr;

    public CaseMapping(FormulaManager fmgr) {
        this.fmgr = fmgr;
        this.bmgr = fmgr.getBooleanFormulaManager();
        this.imgr = fmgr.getIntegerFormulaManager();
        this.smgr = fmgr.getStringFormulaManager();
    }

    /** The mapping of code point cp, concretely. */
    public static int map(Kind kind, int cp) {
        return switch (kind) {
            case LOWER -> Character.toLowerCase(cp);
            case UPPER -> Character.toUpperCase(cp);
            case FOLD -> Character.toLowerCase(Character.toUpperCase(cp));
        };
    }

    /** String.toLowerCase/toUpperCase with a locale whose rules are those of the root locale. */
    static String mapString(Kind kind, String s) {
        return kind == Kind.LOWER ? s.toLowerCase(Locale.ROOT) : s.toUpperCase(Locale.ROOT);
    }

    /**
     * Whether String.toLowerCase/toUpperCase with this locale follows the root locale's rules:
     * Java has special rules only for Turkish, Azerbaijani and Lithuanian.
     */
    public static boolean rootRules(Locale locale) {
        String lang = locale.getLanguage();
        return !lang.equals("tr") && !lang.equals("az") && !lang.equals("lt");
    }

    /** The mapping of code point cp (a formula over the integers). */
    public IntegerFormula apply(Kind kind, IntegerFormula cp) {
        List<Segment> segments = Tables.SEGMENTS.get(kind);
        return apply(segments, 0, segments.size(), cp);
    }

    /** A balanced decision over the segments [from, to), sorted by their codes. */
    private IntegerFormula apply(List<Segment> segments, int from, int to, IntegerFormula cp) {
        if (from == to) return cp;
        if (to - from == 1) {
            Segment s = segments.get(from);
            BooleanFormula in = bmgr.and(imgr.greaterOrEquals(cp, imgr.makeNumber(s.lo())),
                    imgr.lessOrEquals(cp, imgr.makeNumber(s.hi())));
            if (s.stride() == 2) {
                in = bmgr.and(in, imgr.equal(imgr.modulo(imgr.subtract(cp, imgr.makeNumber(s.lo())), imgr.makeNumber(2)),
                        imgr.makeNumber(0)));
            }
            return bmgr.ifThenElse(in, imgr.add(cp, imgr.makeNumber(s.delta())), cp);
        }
        int mid = (from + to) / 2;
        return bmgr.ifThenElse(imgr.lessThan(cp, imgr.makeNumber(segments.get(mid).lo())),
                apply(segments, from, mid, cp), apply(segments, mid, to, cp));
    }

    /**
     * The chars String.toLowerCase/toUpperCase (kind LOWER or UPPER) maps the char c (not a
     * surrogate; for LOWER not 'Σ', whose mapping depends on the chars around it) to.
     */
    public StringFormula charString(Kind kind, IntegerFormula c) {
        StringFormula result = smgr.fromCodePoint(apply(kind, c));
        for (Map.Entry<Integer, String> special : Tables.SPECIALS.get(kind).descendingMap().entrySet()) {
            result = bmgr.ifThenElse(imgr.equal(c, imgr.makeNumber(special.getKey())), chars(special.getValue()), result);
        }
        return result;
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
     * The strings equal to t ignoring case char by char: at each position, the chars with the same
     * FOLD key as t's char there.
     */
    public RegexFormula equalIgnoringCase(String t) {
        RegexFormula regex = smgr.makeRegex("");
        for (char c : t.toCharArray()) {
            RegexFormula chars = null;
            for (int other : Tables.FOLD_CLASSES.get(map(Kind.FOLD, c))) {
                RegexFormula one = smgr.range((char) other, (char) other);
                chars = chars == null ? one : smgr.union(chars, one);
            }
            regex = smgr.concat(regex, chars);
        }
        return regex;
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

    /** Character.highSurrogate(cp) of a supplementary code point */
    public IntegerFormula highSurrogate(IntegerFormula cp) {
        return imgr.add(imgr.makeNumber(Character.MIN_HIGH_SURROGATE),
                imgr.divide(imgr.subtract(cp, imgr.makeNumber(Character.MIN_SUPPLEMENTARY_CODE_POINT)), imgr.makeNumber(1024)));
    }

    /** Character.lowSurrogate(cp) of a supplementary code point */
    public IntegerFormula lowSurrogate(IntegerFormula cp) {
        return imgr.add(imgr.makeNumber(Character.MIN_LOW_SURROGATE),
                imgr.modulo(imgr.subtract(cp, imgr.makeNumber(Character.MIN_SUPPLEMENTARY_CODE_POINT)), imgr.makeNumber(1024)));
    }

    /** The code of the char of s at index i */
    public IntegerFormula codeAt(StringFormula s, IntegerFormula i) {
        return smgr.toCodePoint(smgr.charAt(s, i));
    }

    /**
     * String.toLowerCase/toUpperCase (kind LOWER or UPPER) of the first n chars of s, for a locale
     * with the root locale's rules. For LOWER, s must not contain 'Σ'. A surrogate pair is mapped as
     * its code point (whose mapping is again a supplementary code point), a lone surrogate stays.
     */
    public StringFormula mapChars(Kind kind, StringFormula s, int n) {
        IntegerFormula[] c = new IntegerFormula[n];
        for (int i = 0; i < n; i++) c[i] = codeAt(s, imgr.makeNumber(i));
        StringFormula result = smgr.makeString("");
        for (int i = 0; i < n; i++) {
            StringFormula chars = charString(kind, c[i]);
            if (i > 0) {
                // the low surrogate of a pair: the low surrogate of the pair's mapping
                chars = bmgr.ifThenElse(bmgr.and(isLowSurrogate(c[i]), isHighSurrogate(c[i - 1])),
                        smgr.fromCodePoint(lowSurrogate(apply(kind, toCodePoint(c[i - 1], c[i])))), chars);
            }
            if (i + 1 < n) {
                chars = bmgr.ifThenElse(bmgr.and(isHighSurrogate(c[i]), isLowSurrogate(c[i + 1])),
                        smgr.fromCodePoint(highSurrogate(apply(kind, toCodePoint(c[i], c[i + 1])))), chars);
            }
            result = smgr.concat(result, chars);
        }
        return result;
    }

    /** The result of an ignore-case comparison: whether the loop ended without a difference, and the int it returns. */
    public record Comparison(BooleanFormula ended, IntegerFormula value) {}

    /**
     * Java's ignore-case comparison of a[aStart, aStart + tlen) with b[bStart, bStart + olen)
     * (StringUTF16.compareToCIImpl, which regionMatches and equalsIgnoreCase use too), unrolled
     * over the concrete lengths. Equal chars and chars with the same FOLD key match; otherwise the
     * code points the two chars belong to are compared, and a high surrogate that starts a pair
     * moves past the pair. Since that may move the two indices apart, the loop is unrolled over
     * the pairs of indices it may reach.
     */
    public Comparison compareIgnoringCase(StringFormula a, IntegerFormula aStart, int tlen,
                                          StringFormula b, IntegerFormula bStart, int olen) {
        Side sa = new Side(a, aStart, tlen);
        Side sb = new Side(b, bStart, olen);
        // reach[k1][k2]: the loop gets to k1, k2 with no difference before
        BooleanFormula[][] reach = new BooleanFormula[tlen + 1][olen + 1];
        reach[0][0] = bmgr.makeTrue();
        BooleanFormula ended = bmgr.makeFalse();
        List<BooleanFormula> differs = new ArrayList<>();
        List<IntegerFormula> differences = new ArrayList<>();
        for (int sum = 0; sum <= tlen + olen; sum++) {
            for (int k1 = Math.max(0, sum - olen); k1 <= Math.min(tlen, sum); k1++) {
                int k2 = sum - k1;
                BooleanFormula here = reach[k1][k2];
                if (here == null) continue;
                if (k1 == tlen || k2 == olen) {
                    ended = bmgr.or(ended, here);
                    continue;
                }
                BooleanFormula same = imgr.equal(sa.fold(k1), sb.fold(k2));
                step(reach, k1 + 1, k2 + 1, bmgr.and(here, same));
                BooleanFormula mismatch = bmgr.and(here, bmgr.not(same));
                BooleanFormula sameCodePoint = imgr.equal(sa.foldCodePoint(k1), sb.foldCodePoint(k2));
                for (int skip1 = 0; skip1 <= (sa.canSkip(k1) ? 1 : 0); skip1++) {
                    for (int skip2 = 0; skip2 <= (sb.canSkip(k2) ? 1 : 0); skip2++) {
                        step(reach, k1 + 1 + skip1, k2 + 1 + skip2, bmgr.and(mismatch, sameCodePoint,
                                skip1 == 1 ? sa.skip(k1) : bmgr.not(sa.skip(k1)),
                                skip2 == 1 ? sb.skip(k2) : bmgr.not(sb.skip(k2))));
                    }
                }
                differs.add(bmgr.and(mismatch, bmgr.not(sameCodePoint)));
                differences.add(imgr.subtract(sa.foldCodePoint(k1), sb.foldCodePoint(k2)));
            }
        }
        // Exactly one of the differences, or the end, is reached.
        IntegerFormula value = imgr.makeNumber(tlen - olen);
        for (int i = differs.size() - 1; i >= 0; i--) {
            value = bmgr.ifThenElse(differs.get(i), differences.get(i), value);
        }
        return new Comparison(ended, value);
    }

    /**
     * The comparison of compareToIgnoreCase for strings of different coders (one Latin-1, i.e. all
     * chars below 256, the other not; StringLatin1.compareToCI_UTF16): char by char, by the FOLD keys
     * of the chars, without surrogate pairs.
     */
    public IntegerFormula compareCharsIgnoringCase(StringFormula a, int tlen, StringFormula b, int olen) {
        Side sa = new Side(a, imgr.makeNumber(0), tlen);
        Side sb = new Side(b, imgr.makeNumber(0), olen);
        IntegerFormula value = imgr.makeNumber(tlen - olen);
        for (int k = Math.min(tlen, olen) - 1; k >= 0; k--) {
            value = bmgr.ifThenElse(imgr.equal(sa.fold(k), sb.fold(k)), value, imgr.subtract(sa.fold(k), sb.fold(k)));
        }
        return value;
    }

    /** Whether all chars of s are below 256: a String with a Latin-1 coder. */
    public BooleanFormula isLatin1(StringFormula s) {
        return smgr.in(s, smgr.closure(smgr.range('\u0000', 'ÿ')));
    }

    private void step(BooleanFormula[][] reach, int k1, int k2, BooleanFormula condition) {
        reach[k1][k2] = reach[k1][k2] == null ? condition : bmgr.or(reach[k1][k2], condition);
    }

    /** One of the regions of a comparison, with the terms of its chars made once. */
    private final class Side {
        final IntegerFormula[] code;
        final IntegerFormula[] fold;
        final IntegerFormula[] foldCodePoint;
        final BooleanFormula[] skip;
        final int length;

        Side(StringFormula s, IntegerFormula start, int length) {
            this.length = length;
            code = new IntegerFormula[length];
            fold = new IntegerFormula[length];
            foldCodePoint = new IntegerFormula[length];
            skip = new BooleanFormula[length];
            for (int k = 0; k < length; k++) code[k] = codeAt(s, imgr.add(start, imgr.makeNumber(k)));
        }

        IntegerFormula fold(int k) {
            if (fold[k] == null) fold[k] = apply(Kind.FOLD, code[k]);
            return fold[k];
        }

        /** As StringUTF16.codePointIncluding: a low surrogate after a high one, or a high one before a low one. */
        private BooleanFormula lowWithHigh(int k) {
            return k > 0 ? bmgr.and(isLowSurrogate(code[k]), isHighSurrogate(code[k - 1])) : bmgr.makeFalse();
        }

        boolean canSkip(int k) {
            return k + 1 < length;
        }

        BooleanFormula skip(int k) {
            if (skip[k] == null) {
                skip[k] = canSkip(k) ? bmgr.and(isHighSurrogate(code[k]), isLowSurrogate(code[k + 1])) : bmgr.makeFalse();
            }
            return skip[k];
        }

        IntegerFormula foldCodePoint(int k) {
            if (foldCodePoint[k] == null) {
                IntegerFormula cp = code[k];
                if (canSkip(k)) cp = bmgr.ifThenElse(skip(k), toCodePoint(code[k], code[k + 1]), cp);
                if (k > 0) cp = bmgr.ifThenElse(lowWithHigh(k), toCodePoint(code[k - 1], code[k]), cp);
                foldCodePoint[k] = apply(Kind.FOLD, cp);
            }
            return foldCodePoint[k];
        }
    }
}
