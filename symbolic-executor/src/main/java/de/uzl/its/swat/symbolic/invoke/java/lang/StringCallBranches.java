package de.uzl.its.swat.symbolic.invoke.java.lang;

import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.StringBuilderValue;
import de.uzl.its.swat.symbolic.value.reference.array.CharArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverContext;
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
 *   <li>Pins: a model that works on the concrete value of a symbolic int (e.g. a loop over the
 *       concrete length of a string) is exact only for that value. The branch {@code x == concrete}
 *       makes the solver try the other values, in other runs.
 * </ul>
 *
 * Index checks come first: the pins are only reached when the indices are in range.
 */
public final class StringCallBranches {

    /** A branch: its condition and whether it held in this run. */
    public record Branch(BooleanFormula condition, boolean holds) {}

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
            case "lastIndexOf(I)I", "lastIndexOf(II)I" -> pin(num(args[0]));
            default -> { }
        }
    }

    private void ofStringBuilder(StringValue s, String name, String desc, Value<?, ?>[] args) throws Exception {
        String sig = name + desc.substring(0, desc.indexOf(')') + 1);
        switch (sig) {
            case "charAt(I)", "setCharAt(IC)", "deleteCharAt(I)" -> index(s, args[0]);
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

    private void add(BooleanFormula condition, boolean holds) {
        branches.add(new Branch(condition, holds));
    }
}
