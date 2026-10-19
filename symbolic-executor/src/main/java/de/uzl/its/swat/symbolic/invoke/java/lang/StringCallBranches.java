package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.StringBuilderValue;
import de.uzl.its.swat.symbolic.value.reference.array.CharArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.CaseMapping;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormula;
import org.sosy_lab.java_smt.api.StringFormulaManager;

/**
 * The branches a call of a String or StringBuilder method takes inside the library, which the
 * models of these methods (StringValue, StringBuilderValue) rely on. The visitor records them in the
 * trace, so the solver can take their other side:
 *
 * <ul>
 *   <li>Index checks: the method returns normally iff its indices are in range, and throws an
 *       IndexOutOfBoundsException otherwise. One check per exception the method may throw, in the
 *       order Java checks them, so all inputs that fail the same check throw the same exception.
 *   <li>Splits: a model with several cases (e.g. strings of equal and of different lengths)
 *       records which one the call took.
 *   <li>Pins: a model that works on the concrete value of a symbolic int (e.g. a loop over the
 *       concrete length of a string) is exact only for that value. The branch {@code x == concrete}
 *       makes the solver try the other values, in other runs.
 * </ul>
 *
 * Index checks come first: the pins are only reached when the indices are in range.
 */
public final class StringCallBranches {

    /**
     * A branch: its condition and whether it held in this run, and whether it is a check the call
     * throws at if it fails (else the model only distinguishes the cases, and both sides return).
     */
    public record Branch(BooleanFormula condition, boolean holds, boolean check) {}

    /** An int the checks are about, as a formula over the integers and concretely. */
    private record Int(IntegerFormula f, long c, boolean symbolic) {}

    private final SolverContext context;
    private final BooleanFormulaManager bmgr;
    private final IntegerFormulaManager imgr;
    private final StringFormulaManager smgr;
    private final List<Branch> branches = new ArrayList<>();

    private StringCallBranches(SolverContext context) {
        this.context = context;
        FormulaManager fmgr = context.getFormulaManager();
        this.bmgr = fmgr.getBooleanFormulaManager();
        this.imgr = fmgr.getIntegerFormulaManager();
        this.smgr = fmgr.getStringFormulaManager();
    }

    /**
     * The branches of calling {@code name} with descriptor {@code desc} on {@code instance}, in
     * order: empty if the method has none or is not modelled. The concrete values are those before
     * the call.
     */
    public static List<Branch> of(Value<?, ?> instance, String name, String desc, Value<?, ?>[] args) {
        try {
            if (instance instanceof StringValue str) {
                StringCallBranches b = new StringCallBranches(str.context);
                b.ofString(str, name, desc, args);
                return b.branches;
            } else if (instance instanceof StringBuilderValue sb && sb.getContent() != null) {
                StringCallBranches b = new StringCallBranches(sb.context);
                b.ofStringBuilder(sb.getContent(), name, desc, args);
                return b.branches;
            }
        } catch (Exception e) {
            // Arguments of unexpected types: the model returns a PlaceHolder for them as well.
        }
        return List.of();
    }

    private void ofString(StringValue s, String name, String desc, Value<?, ?>[] args) throws Exception {
        switch (name + desc) {
            case "charAt(I)C" -> index(s, args[0]);
            case "substring(I)Ljava/lang/String;" -> range(s, num(args[0]), length(s));
            case "substring(II)Ljava/lang/String;", "subSequence(II)Ljava/lang/CharSequence;" ->
                    range(s, num(args[0]), num(args[1]));
            case "getChars(II[CI)V" -> getChars(s, args);
            case "toCharArray()[C" -> pin(length(s));
            case "<init>([C)V" -> pin(size(args[0]));
            case "<init>([CII)V" -> {
                // checkBoundsOffCount(offset, count, value.length), then the chars by the concrete
                // offset and count
                Int offset = num(args[1]);
                Int count = num(args[2]);
                Int size = size(args[0]);
                add(bmgr.and(imgr.greaterOrEquals(offset.f(), zero()), imgr.greaterOrEquals(count.f(), zero()),
                                imgr.lessOrEquals(offset.f(), imgr.subtract(size.f(), count.f()))),
                        offset.c() >= 0 && count.c() >= 0 && offset.c() <= size.c() - count.c());
                pin(offset);
                pin(count);
                pin(size);
            }
            case "lastIndexOf(I)I", "lastIndexOf(II)I" -> pin(num(args[0]));
            case "codePointAt(I)I" -> index(s, args[0]);
            case "codePointBefore(I)I" -> codePointBefore(s, args[0]);
            case "codePointCount(II)I" -> codePointCount(s, args);
            case "repeat(I)Ljava/lang/String;" -> {
                Int count = num(args[0]);
                add(imgr.greaterOrEquals(count.f(), zero()), count.c() >= 0);
                if (count.c() >= 0) pin(count);
            }
            case "equalsIgnoreCase(Ljava/lang/String;)Z" -> {
                if (args[0] instanceof StringValue other && (s.isSymbolic() || other.isSymbolic())
                        && !StringValue.equalsIgnoreCaseByRegex(s, other) && ascii(s, other)) {
                    // Strings of different lengths differ, else the comparison by the pinned length
                    Int len = length(s);
                    Int otherLen = length(other);
                    split(imgr.equal(len.f(), otherLen.f()), len.c() == otherLen.c());
                    if (len.c() == otherLen.c()) pin(len);
                }
            }
            case "regionMatches(ZILjava/lang/String;II)Z" -> {
                // Ignoring case (maybe): the bounds check, then the comparison by the pinned len
                BooleanValue ignoreCase = args[0].asBooleanValue();
                if ((ignoreCase.isSymbolic() || ignoreCase.concrete) && args[2] instanceof StringValue other) {
                    Int toffset = num(args[1]);
                    Int ooffset = num(args[3]);
                    Int len = num(args[4]);
                    boolean inBounds = StringValue.regionInBounds(s.concrete, (int) toffset.c(), other.concrete, (int) ooffset.c(), (int) len.c());
                    split(bmgr.and(imgr.greaterOrEquals(ooffset.f(), zero()), imgr.greaterOrEquals(toffset.f(), zero()),
                                    imgr.lessOrEquals(toffset.f(), imgr.subtract(length(s).f(), len.f())),
                                    imgr.lessOrEquals(ooffset.f(), imgr.subtract(length(other).f(), len.f()))),
                            inBounds);
                    if (inBounds && ascii(s, other)) pin(len);
                }
            }
            case "compareToIgnoreCase(Ljava/lang/String;)I" -> {
                if (args[0] instanceof StringValue other && ascii(s, other)) {
                    pin(length(s));
                    pin(length(other));
                }
            }
            case "toLowerCase()Ljava/lang/String;", "toLowerCase(Ljava/util/Locale;)Ljava/lang/String;",
                 "toUpperCase()Ljava/lang/String;", "toUpperCase(Ljava/util/Locale;)Ljava/lang/String;" -> {
                if (StringValue.caseMappingLocale(args) != null && ascii(s)) pin(length(s));
            }
            default -> { }
        }
    }

    private void ofStringBuilder(StringValue s, String name, String desc, Value<?, ?>[] args) throws Exception {
        String sig = name + desc.substring(0, desc.indexOf(')') + 1);
        switch (sig) {
            case "charAt(I)", "setCharAt(IC)", "deleteCharAt(I)", "codePointAt(I)" -> index(s, args[0]);
            case "codePointBefore(I)" -> codePointBefore(s, args[0]);
            case "codePointCount(II)" -> codePointCount(s, args);
            case "reverse()" -> pin(length(s));
            case "delete(II)", "replace(IILjava/lang/String;)" -> {
                // An end past the length is taken as the length: start in [0, min(end, length)].
                Int start = num(args[0]);
                Int end = num(args[1]);
                Int len = length(s);
                add(bmgr.and(imgr.greaterOrEquals(start.f(), zero()), imgr.lessOrEquals(start.f(), end.f()),
                                imgr.lessOrEquals(start.f(), len.f())),
                        0 <= start.c() && start.c() <= end.c() && start.c() <= len.c());
            }
            case "substring(I)" -> range(s, num(args[0]), length(s));
            case "substring(II)", "subSequence(II)" -> range(s, num(args[0]), num(args[1]));
            case "setLength(I)" -> {
                Int n = num(args[0]);
                add(imgr.greaterOrEquals(n.f(), zero()), n.c() >= 0);
                pin(n);
            }
            case "getChars(II[CI)" -> getChars(s, args);
            case "appendCodePoint(I)" -> {
                Int cp = num(args[0]);
                add(bmgr.and(imgr.greaterOrEquals(cp.f(), zero()), imgr.lessOrEquals(cp.f(), imgr.makeNumber(Character.MAX_CODE_POINT))),
                        Character.isValidCodePoint((int) cp.c()));
            }
            case "append([C)" -> pin(size(args[0]));
            case "append([CII)" -> {
                // checkRange(offset, offset + len, str.length), in int arithmetic: with all three
                // pinned, the check is concrete.
                pin(num(args[1]));
                pin(num(args[2]));
                pin(size(args[0]));
            }
            case "append(Ljava/lang/CharSequence;II)" -> {
                StringValue seq = sequence(args[0]);
                if (seq != null) range(seq, num(args[1]), num(args[2]));
            }
            default -> {
                if (name.equals("insert")) insert(s, desc, args);
            }
        }
    }

    private void insert(StringValue s, String desc, Value<?, ?>[] args) throws Exception {
        // insert(int offset, ...): the offset in [0, length]
        Int offset = num(args[0]);
        Int len = length(s);
        add(bmgr.and(imgr.greaterOrEquals(offset.f(), zero()), imgr.lessOrEquals(offset.f(), len.f())),
                0 <= offset.c() && offset.c() <= len.c());
        if (desc.startsWith("(I[CII)")) {
            pin(num(args[2]));
            pin(num(args[3]));
            pin(size(args[1]));
        } else if (desc.startsWith("(I[C)")) {
            pin(size(args[1]));
        } else if (desc.startsWith("(ILjava/lang/CharSequence;II)")) {
            StringValue seq = sequence(args[1]);
            if (seq != null) range(seq, num(args[2]), num(args[3]));
        }
    }

    /** getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) of String and StringBuilder. */
    private void getChars(StringValue s, Value<?, ?>[] args) throws Exception {
        Int srcBegin = num(args[0]);
        Int srcEnd = num(args[1]);
        range(s, srcBegin, srcEnd);
        if (!(args[2] instanceof CharArrayValue)) return;
        Int dstBegin = num(args[3]);
        Int dstLength = size(args[2]);
        // dst[dstBegin, dstBegin + srcEnd - srcBegin) in range (srcBegin <= srcEnd holds here)
        IntegerFormula end = imgr.add(dstBegin.f(), imgr.subtract(srcEnd.f(), srcBegin.f()));
        add(bmgr.and(imgr.greaterOrEquals(dstBegin.f(), zero()), imgr.lessOrEquals(end, dstLength.f())),
                dstBegin.c() >= 0 && dstBegin.c() + srcEnd.c() - srcBegin.c() <= dstLength.c());
        // The model copies by the concrete indices.
        pin(srcBegin);
        pin(srcEnd);
        pin(dstBegin);
    }

    /**
     * Whether the strings are ASCII, the case the case-insensitive models cover (see CaseMapping):
     * a split between ASCII strings and the others.
     */
    private boolean ascii(StringValue... strings) {
        CaseMapping cases = new CaseMapping(context.getFormulaManager());
        BooleanFormula all = bmgr.makeTrue();
        for (StringValue str : strings) all = bmgr.and(all, cases.isAscii(str.formula));
        boolean holds = StringValue.asciiStrings(strings);
        split(all, holds);
        return holds;
    }

    /** codePointBefore(int index): 0 <= index - 1 < length of s */
    private void codePointBefore(StringValue s, Value<?, ?> indexArg) throws Exception {
        Int i = num(indexArg);
        Int len = length(s);
        add(bmgr.and(imgr.greaterOrEquals(i.f(), imgr.makeNumber(1)), imgr.lessOrEquals(i.f(), len.f())),
                1 <= i.c() && i.c() <= len.c());
    }

    /** codePointCount(int begin, int end): 0 <= begin <= end <= length of s, and end - begin pinned */
    private void codePointCount(StringValue s, Value<?, ?>[] args) throws Exception {
        Int begin = num(args[0]);
        Int end = num(args[1]);
        range(s, begin, end);
        if (0 <= begin.c() && begin.c() <= end.c() && end.c() <= s.concrete.length()) {
            pin(new Int(imgr.subtract(end.f(), begin.f()), end.c() - begin.c(), begin.symbolic() || end.symbolic()));
        }
    }

    /** 0 <= index < length of s */
    private void index(StringValue s, Value<?, ?> indexArg) throws Exception {
        Int i = num(indexArg);
        Int len = length(s);
        add(bmgr.and(imgr.greaterOrEquals(i.f(), zero()), imgr.lessThan(i.f(), len.f())),
                0 <= i.c() && i.c() < len.c());
    }

    /** 0 <= begin <= end <= length of s */
    private void range(StringValue s, Int b, Int e) {
        Int len = length(s);
        add(bmgr.and(imgr.greaterOrEquals(b.f(), zero()), imgr.lessOrEquals(b.f(), e.f()), imgr.lessOrEquals(e.f(), len.f())),
                0 <= b.c() && b.c() <= e.c() && e.c() <= len.c());
    }

    private Int num(Value<?, ?> v) throws Exception {
        IntValue i = v.asIntValue();
        return new Int(i.asIntegerFormula(), i.concrete, i.isSymbolic());
    }

    private Int length(StringValue s) {
        return new Int(smgr.length(s.formula), s.concrete.length(), s.isSymbolic());
    }

    private Int size(Value<?, ?> array) throws Exception {
        return num(((CharArrayValue) array).size);
    }

    /** The chars of a CharSequence argument; null is taken as "null", as by append and insert. */
    private StringValue sequence(Value<?, ?> v) {
        if (v instanceof ObjectValue<?, ?> o && o.isNullValue()) return new StringValue(context, "null", -1);
        return StringValue.charsOf(v);
    }

    /** x == its concrete value, if x is symbolic */
    private void pin(Int x) {
        if (x.symbolic()) {
            add(imgr.equal(x.f(), imgr.makeNumber(x.c())), true);
        }
    }

    private IntegerFormula zero() {
        return imgr.makeNumber(0);
    }

    /** A check: the call throws if it fails. */
    private void add(BooleanFormula condition, boolean holds) {
        branches.add(new Branch(condition, holds, true));
    }

    /** A branch between the cases of a model, which both return. */
    private void split(BooleanFormula condition, boolean holds) {
        branches.add(new Branch(condition, holds, false));
    }
}
