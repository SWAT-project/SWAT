package de.uzl.its.swat.symbolic.value.reference.lang;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.java_smt.api.RegexFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

/**
 * Translates the simple Java regular expressions (java.util.regex.Pattern, no flags) whose
 * language an SMT regular expression describes exactly: literal chars, the escaped
 * metacharacters, positive character classes ([a-z0-9_], \d, \s, \w), groups, alternation and the
 * greedy and reluctant quantifiers. Their atoms match only chars outside the surrogates, so a
 * surrogate (alone or in a pair, which Java matches as one code point) matches none of them, char
 * by char as in Java.
 *
 * <p>Everything else (".", negated classes, anchors, back references, lookaround, possessive
 * quantifiers, inline flags, ...) is not translated: {@link #toRegex} then returns null.
 */
public final class JavaRegex {
    private static final String METACHARS = ".$^|?*+()[]{}\\";

    private final StringFormulaManager smgr;
    private final String pattern;
    private int pos = 0;

    private JavaRegex(StringFormulaManager smgr, String pattern) {
        this.smgr = smgr;
        this.pattern = pattern;
    }

    /** The SMT regular expression for the Java one, or null if it is not one of the simple ones. */
    public static RegexFormula toRegex(StringFormulaManager smgr, String pattern) {
        JavaRegex parser = new JavaRegex(smgr, pattern);
        try {
            RegexFormula regex = parser.alternation();
            return parser.pos == pattern.length() ? regex : null;
        } catch (Unsupported e) {
            return null;
        }
    }

    /**
     * The literal a regular expression stands for if it has no construct but literal chars and
     * escaped metacharacters (e.g. "\\*"), else null.
     */
    public static String literal(String pattern) {
        StringBuilder chars = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\\') {
                if (i + 1 == pattern.length()) return null;
                char next = pattern.charAt(++i);
                if (Character.isLetterOrDigit(next) || Character.isSurrogate(next)) return null;
                chars.append(next);
            } else if (METACHARS.indexOf(c) >= 0 || Character.isSurrogate(c)) {
                return null;
            } else {
                chars.append(c);
            }
        }
        return chars.toString();
    }

    /** Whether a replacement string of replaceAll/replaceFirst stands for itself (no '$' or '\'). */
    public static boolean literalReplacement(String replacement) {
        return replacement.indexOf('$') < 0 && replacement.indexOf('\\') < 0;
    }

    private static final class Unsupported extends Exception {
        Unsupported() {
            super(null, null, false, false);
        }
    }

    private boolean more() {
        return pos < pattern.length();
    }

    private char peek() {
        return pattern.charAt(pos);
    }

    private RegexFormula alternation() throws Unsupported {
        RegexFormula regex = sequence();
        while (more() && peek() == '|') {
            pos++;
            regex = smgr.union(regex, sequence());
        }
        return regex;
    }

    private RegexFormula sequence() throws Unsupported {
        List<RegexFormula> parts = new ArrayList<>();
        while (more() && peek() != '|' && peek() != ')') {
            parts.add(quantified(atom()));
        }
        if (parts.isEmpty()) return smgr.makeRegex("");
        return parts.size() == 1 ? parts.get(0) : smgr.concat(parts.toArray(new RegexFormula[0]));
    }

    private RegexFormula quantified(RegexFormula atom) throws Unsupported {
        RegexFormula regex = atom;
        if (!more()) return regex;
        char c = peek();
        if (c == '*') {
            pos++;
            regex = smgr.closure(atom);
        } else if (c == '+') {
            pos++;
            regex = smgr.cross(atom);
        } else if (c == '?') {
            pos++;
            regex = smgr.optional(atom);
        } else if (c == '{') {
            pos++;
            int min = number();
            int max = min;
            if (more() && peek() == ',') {
                pos++;
                max = more() && peek() == '}' ? -1 : number();
            }
            expect('}');
            if (max != -1 && max < min) throw new Unsupported();
            regex = repeat(atom, min);
            if (max == -1) {
                regex = smgr.concat(regex, smgr.closure(atom));
            } else {
                for (int i = min; i < max; i++) regex = smgr.concat(regex, smgr.optional(atom));
            }
        } else {
            return regex;
        }
        // Reluctant matching does not change which strings match; possessive matching does.
        if (more() && peek() == '?') {
            pos++;
        } else if (more() && peek() == '+') {
            throw new Unsupported();
        }
        if (more() && (peek() == '*' || peek() == '+' || peek() == '?' || peek() == '{')) throw new Unsupported();
        return regex;
    }

    /** atom, n times */
    private RegexFormula repeat(RegexFormula atom, int n) {
        RegexFormula regex = smgr.makeRegex("");
        for (int i = 0; i < n; i++) regex = smgr.concat(regex, atom);
        return regex;
    }

    private int number() throws Unsupported {
        int start = pos;
        while (more() && Character.isDigit(peek()) && peek() < 128) pos++;
        if (pos == start || pos - start > 4) throw new Unsupported();
        return Integer.parseInt(pattern.substring(start, pos));
    }

    private void expect(char c) throws Unsupported {
        if (!more() || peek() != c) throw new Unsupported();
        pos++;
    }

    private RegexFormula atom() throws Unsupported {
        char c = peek();
        switch (c) {
            case '(' -> {
                pos++;
                if (more() && peek() == '?') {
                    // only the non-capturing group (?:...)
                    pos++;
                    expect(':');
                }
                RegexFormula inner = alternation();
                expect(')');
                return inner;
            }
            case '[' -> {
                pos++;
                return characterClass();
            }
            case '\\' -> {
                pos++;
                return escape(false);
            }
            default -> {
                if (METACHARS.indexOf(c) >= 0 || c == ']' || c == '}') throw new Unsupported();
                pos++;
                return single(c);
            }
        }
    }

    private RegexFormula single(char c) throws Unsupported {
        if (Character.isSurrogate(c)) throw new Unsupported();
        return smgr.range(c, c);
    }

    /** After a backslash: a predefined class, a control char, or an escaped non-alphanumeric char. */
    private RegexFormula escape(boolean inClass) throws Unsupported {
        if (!more()) throw new Unsupported();
        char c = pattern.charAt(pos++);
        return switch (c) {
            case 'd' -> smgr.range('0', '9');
            case 's' -> smgr.union(smgr.range(' ', ' '), smgr.range('\t', '\r')); // [ \t\n\x0B\f\r]
            case 'w' -> smgr.union(smgr.union(smgr.range('a', 'z'), smgr.range('A', 'Z')),
                    smgr.union(smgr.range('0', '9'), smgr.range('_', '_')));
            case 't' -> single('\t');
            case 'n' -> single('\n');
            case 'r' -> single('\r');
            case 'f' -> single('\f');
            default -> {
                if (Character.isLetterOrDigit(c) || Character.isSurrogate(c)) throw new Unsupported();
                yield single(c);
            }
        };
    }

    /** A positive class [...] of chars, ranges and predefined classes. */
    private RegexFormula characterClass() throws Unsupported {
        if (more() && peek() == '^') throw new Unsupported();
        RegexFormula regex = null;
        boolean first = true;
        while (more() && (peek() != ']' || first)) {
            char c = peek();
            RegexFormula part;
            if (c == '[' || (c == '&' && pos + 1 < pattern.length() && pattern.charAt(pos + 1) == '&')) {
                throw new Unsupported();
            } else if (c == '\\') {
                pos++;
                char next = more() ? peek() : 0;
                if (next == 'd' || next == 's' || next == 'w') {
                    part = escape(true);
                    if (pos + 1 < pattern.length() && peek() == '-' && pattern.charAt(pos + 1) != ']') throw new Unsupported();
                } else {
                    part = rangeFrom(escapedChar());
                }
            } else {
                pos++;
                part = rangeFrom(c);
            }
            regex = regex == null ? part : smgr.union(regex, part);
            first = false;
        }
        expect(']');
        if (regex == null) throw new Unsupported();
        return regex;
    }

    /** The char of an escape inside a class (not a predefined class). */
    private char escapedChar() throws Unsupported {
        if (!more()) throw new Unsupported();
        char c = pattern.charAt(pos++);
        return switch (c) {
            case 't' -> '\t';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 'f' -> '\f';
            default -> {
                if (Character.isLetterOrDigit(c) || Character.isSurrogate(c)) throw new Unsupported();
                yield c;
            }
        };
    }

    /** The char from, or the range from-to if a '-' and an end follow. */
    private RegexFormula rangeFrom(char from) throws Unsupported {
        if (pos + 1 < pattern.length() && peek() == '-' && pattern.charAt(pos + 1) != ']') {
            pos++;
            char to;
            if (peek() == '\\') {
                pos++;
                to = escapedChar();
            } else if (peek() == '[') {
                throw new Unsupported();
            } else {
                to = pattern.charAt(pos++);
            }
            if (to < from || Character.isSurrogate(from) || Character.isSurrogate(to)
                    || (from < Character.MIN_SURROGATE && to > Character.MAX_SURROGATE)) {
                throw new Unsupported();
            }
            return smgr.range(from, to);
        }
        return single(from);
    }
}
