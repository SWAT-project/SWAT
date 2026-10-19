package de.uzl.its.swat.symbolic.value.reference;

import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.array.CharArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.BoxedValue;
import de.uzl.its.swat.symbolic.value.reference.lang.Utf16;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import java.util.Set;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

/**
 * Shadow of a <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/StringBuilder.html">StringBuilder</a>
 * or a <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/StringBuffer.html">StringBuffer</a>
 * (the same methods, synchronized): its chars as a {@link StringValue}, which is replaced (never changed: it may be a String the
 * program holds) on every change.
 *
 * <p>The index checks of the methods (when they throw an IndexOutOfBoundsException) and the
 * concrete values the models rely on are recorded by the caller as branches, see
 * StringCallBranches. The models below assume those hold.
 *
 * <p>After a change SWAT does not model (it returned a PlaceHolder), the chars are unknown: every
 * method then returns a PlaceHolder, until the builder is recovered from the heap with its concrete
 * value ({@link #resync}).
 */
public final class StringBuilderValue extends ObjectValue<Object, Object> {
    private final StringFormulaManager smgr;
    private final IntegerFormulaManager imgr;
    private StringValue stringValue;

    /**
     * The capacity while it is the one the constructor set; null after a change that may grow it
     * (the growth policy is not modelled).
     */
    private IntValue capacity;

    /** The methods that do not change the capacity. */
    private static final Set<String> KEEP_CAPACITY = Set.of("charAt", "codePointAt", "codePointBefore",
            "codePointCount", "compareTo", "delete", "deleteCharAt",
            "getChars", "indexOf", "lastIndexOf", "length", "reverse", "setCharAt", "subSequence", "substring",
            "toString", "capacity");

    /** Whether a change SWAT does not model left the chars unknown. */
    private boolean unknown = false;

    /** The methods that change the builder: if one is not modelled, the chars are unknown. */
    private static final Set<String> MUTATORS = Set.of("<init>", "append", "appendCodePoint", "delete",
            "deleteCharAt", "insert", "replace", "reverse", "setCharAt", "setLength");

    private static final String STRING_BUILDER = "java/lang/StringBuilder";

    public StringBuilderValue(SolverContext context) {
        super(context, ObjectValue.ADDRESS_UNKNOWN);
        this.className = STRING_BUILDER;
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();

        this.stringValue = new StringValue(context, "", -1);
    }

    public StringBuilderValue(SolverContext context, StringValue v, int address) {
        this(context, STRING_BUILDER, v, address);
    }

    /** @param className java/lang/StringBuilder or java/lang/StringBuffer */
    public StringBuilderValue(SolverContext context, String className, StringValue v, int address) {
        super(context, address);
        this.className = className;
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.stringValue = v;
    }

    public Value<?, ?> getStringValue() {
        return stringValue;
    }

    /** The chars, or null if they are not known. */
    public StringValue getContent() {
        return unknown ? null : stringValue;
    }

    public boolean isUnknown() {
        return unknown;
    }

    /**
     * Takes the concrete chars of the builder, e.g. when it is recovered from the heap after a
     * change SWAT does not model. A symbolic content was lost at that change already (with a
     * context loss: the call returned a PlaceHolder for a symbolic receiver).
     */
    public void resync(String chars) {
        this.stringValue = new StringValue(context, chars, -1);
        this.unknown = false;
    }

    /**
     * Handles method invocation for Java's <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/StringBuilder.html">StringBuilder</a>
     * (Java 16) as defined in:
     *
     * @param name The name of the method that is called
     * @param desc The Type descriptions for all Arguments
     * @param args The Value's representing the arguments
     * @return The return Value of the Method, or a PlaceHolder::instance if the Method is not
     *     implemented or void should be returned
     */
    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        if ((unknown || stringValue == null) && !name.equals("<init>")) return PlaceHolder.instance;
        Value<?, ?> result;
        try {
            result = switch (name) {
                case "<init>" -> invokeInit(args, desc);
                case "append" -> invokeAppend(args, desc);
                case "appendCodePoint" -> invokeAppendCodePoint(args);
                case "capacity" -> capacity != null ? capacity : PlaceHolder.instance;
                case "charAt" -> invokeCharAt(args);
                case "compareTo" -> invokeCompareTo(args);
                case "delete" -> invokeDelete(args);
                case "deleteCharAt" -> invokeDeleteCharAt(args);
                case "ensureCapacity", "trimToSize" -> VoidValue.instance; // the chars stay the same
                case "getChars" -> invokeGetChars(args);
                case "indexOf", "lastIndexOf", "codePointAt", "codePointBefore", "codePointCount" ->
                        stringValue.invokeMethod(name, desc, args);
                case "insert" -> invokeInsert(args, desc);
                case "length" -> new IntValue(context, stringValue.concrete.length(), smgr.length(stringValue.formula));
                case "replace" -> invokeReplace(args);
                case "reverse" -> invokeReverse();
                case "setCharAt" -> invokeSetCharAt(args);
                case "setLength" -> invokeSetLength(args);
                case "subSequence", "substring" -> invokeSubstring(args);
                case "toString" -> new StringValue(context, stringValue.concrete, stringValue.formula, -1);
                default -> PlaceHolder.instance;
            };
        } catch (NotImplementedException e) {
            // e.g. a double appended, which has no model as a String
            result = PlaceHolder.instance;
        }
        if (!name.equals("<init>") && !KEEP_CAPACITY.contains(name)) {
            capacity = null;
        }
        if (result instanceof PlaceHolder && MUTATORS.contains(name)) {
            unknown = true;
        }
        return result;
    }

    private Value<?, ?> invokeInit(Value<?, ?>[] args, Type[] desc) {
        StringValue chars;
        capacity = null;
        if (args.length == 0) {
            chars = new StringValue(context, "", -1);
            capacity = new IntValue(context, 16);
        } else if (desc[0].getSort() == Type.INT) {
            // new StringBuilder(int capacity)
            chars = new StringValue(context, "", -1);
            capacity = args[0] instanceof IntValue c ? c : null;
        } else if (args.length == 1) {
            // new StringBuilder(String), new StringBuilder(CharSequence): length + 16
            chars = StringValue.charsOf(args[0]);
            if (chars != null) {
                capacity = new IntValue(context, chars.concrete.length() + 16,
                        imgr.add(smgr.length(chars.formula), imgr.makeNumber(16)));
            }
        } else {
            chars = null;
        }
        if (chars == null) return PlaceHolder.instance;
        this.stringValue = chars;
        this.unknown = false;
        return VoidValue.instance;
    }

    private void setChars(String concrete, StringFormula formula) {
        this.stringValue = new StringValue(context, concrete, formula, -1);
    }

    private StringValue concat(StringValue a, StringValue b) {
        return new StringValue(context, a.concrete + b.concrete, smgr.concat(a.formula, b.formula), -1);
    }

    private NumeralFormula.IntegerFormula length() {
        return smgr.length(stringValue.formula);
    }

    /** The chars from begin to end of the content. */
    private StringFormula chars(NumeralFormula.IntegerFormula begin, NumeralFormula.IntegerFormula end) {
        return smgr.substring(stringValue.formula, begin, imgr.subtract(end, begin));
    }

    /**
     * The chars a value of type {@code type} is appended or inserted as (String.valueOf), or null
     * if that is not modelled.
     */
    private StringValue charsOf(Value<?, ?> value, Type type) throws NotImplementedException, ValueConversionException {
        return switch (type.getDescriptor()) {
            case "I" -> value.asIntValue().asStringValue();
            case "Z" -> value.asBooleanValue().asStringValue();
            case "C" -> value.asCharValue().asStringValue();
            case "J" -> value.asLongValue().asStringValue();
            case "D" -> value.isSymbolic() ? null
                    : new StringValue(context, String.valueOf(value.asDoubleValue().concrete), -1);
            case "F" -> value.isSymbolic() ? null
                    : new StringValue(context, String.valueOf(value.asFloatValue().concrete), -1);
            case "[C" -> value instanceof CharArrayValue array ? array.asStringValue() : null;
            default -> objectChars(value);
        };
    }

    /** String.valueOf(obj) for an Object, CharSequence or String argument, or null if not modelled. */
    private StringValue objectChars(Value<?, ?> obj) throws NotImplementedException, ValueConversionException {
        if (obj instanceof ObjectValue<?, ?> o && o.isNullValue()) return new StringValue(context, "null", -1);
        StringValue chars = StringValue.charsOf(obj);
        if (chars != null) return chars;
        if (obj instanceof BoxedValue<?> boxed
                && boxed.invokeMethod("toString", new Type[0], new Value<?, ?>[0]) instanceof StringValue str) {
            return str;
        }
        return null;
    }

    private Value<?, ?> invokeAppend(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        StringValue chars;
        if (args.length == 1) {
            chars = charsOf(args[0], desc[0]);
        } else if (args.length == 3 && args[0] instanceof CharArrayValue array) {
            // append(char[] str, int offset, int len), by the concrete offset and len (pinned)
            int offset = args[1].asIntValue().concrete;
            int len = args[2].asIntValue().concrete;
            chars = new StringValue(context, "", -1);
            for (int i = offset; i < offset + len; i++) {
                chars = concat(chars, array.getElement(new IntValue(context, i)).asStringValue());
            }
        } else if (args.length == 3 && desc[0].getDescriptor().equals("Ljava/lang/CharSequence;")) {
            // append(CharSequence s, int start, int end); s == null appends a part of "null"
            StringValue s = objectChars(args[0]);
            if (s == null) return PlaceHolder.instance;
            IntValue start = args[1].asIntValue();
            IntValue end = args[2].asIntValue();
            chars = new StringValue(context, s.concrete.substring(start.concrete, end.concrete),
                    smgr.substring(s.formula, start.asIntegerFormula(),
                            imgr.subtract(end.asIntegerFormula(), start.asIntegerFormula())), -1);
        } else {
            chars = null;
        }
        if (chars == null) return PlaceHolder.instance;
        setChars(stringValue.concrete + chars.concrete, smgr.concat(stringValue.formula, chars.formula));
        return this;
    }

    private Value<?, ?> invokeAppendCodePoint(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        // A valid code point (else it throws, which StringCallBranches checks)
        IntValue codePoint = args[0].asIntValue();
        String chars = new String(Character.toChars(codePoint.concrete));
        setChars(stringValue.concrete + chars, smgr.concat(stringValue.formula, stringValue.codePointString(codePoint)));
        return this;
    }

    private Value<?, ?> invokeCharAt(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        IntValue index = args[0].asIntValue();
        return new CharValue(context, stringValue.concrete.charAt(index.concrete),
                smgr.charAt(stringValue.formula, index.asIntegerFormula()));
    }

    private Value<?, ?> invokeCompareTo(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        // As the Strings of both: compareTo(StringBuilder another) compares lexicographically too.
        StringValue other = StringValue.charsOf(args[0]);
        if (other == null) return PlaceHolder.instance;
        return stringValue.invokeMethod("compareTo", new Type[] {Type.getType(String.class)}, new Value<?, ?>[] {other});
    }

    private Value<?, ?> invokeDelete(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        IntValue start = args[0].asIntValue();
        IntValue end = args[1].asIntValue();
        // An end past the length deletes up to the end; substring gives "" from there.
        NumeralFormula.IntegerFormula e = end.asIntegerFormula();
        setChars(new StringBuilder(stringValue.concrete).delete(start.concrete, end.concrete).toString(),
                smgr.concat(chars(imgr.makeNumber(0), start.asIntegerFormula()),
                        smgr.substring(stringValue.formula, e, imgr.subtract(length(), e))));
        return this;
    }

    private Value<?, ?> invokeDeleteCharAt(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        IntValue index = args[0].asIntValue();
        IntValue end = new IntValue(context, index.concrete + 1,
                imgr.add(index.asIntegerFormula(), imgr.makeNumber(1)));
        return invokeDelete(new Value<?, ?>[] {index, end});
    }

    private Value<?, ?> invokeGetChars(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        // getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin), by the concrete indices (pinned)
        if (!(args[2] instanceof CharArrayValue dst)) return PlaceHolder.instance;
        int srcBegin = args[0].asIntValue().concrete;
        int srcEnd = args[1].asIntValue().concrete;
        int dstBegin = args[3].asIntValue().concrete;
        for (int i = srcBegin; i < srcEnd; i++) {
            dst.storeElement(new IntValue(context, dstBegin + i - srcBegin),
                    (CharValue) invokeCharAt(new Value<?, ?>[] {new IntValue(context, i)}));
        }
        return VoidValue.instance;
    }

    private Value<?, ?> invokeInsert(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        IntValue offset = args[0].asIntValue();
        StringValue chars;
        if (args.length == 2) {
            chars = charsOf(args[1], desc[1]);
        } else if (args.length == 4 && args[1] instanceof CharArrayValue array) {
            // insert(int index, char[] str, int offset, int len), by the concrete offset and len (pinned)
            int from = args[2].asIntValue().concrete;
            int len = args[3].asIntValue().concrete;
            chars = new StringValue(context, "", -1);
            for (int i = from; i < from + len; i++) {
                chars = concat(chars, array.getElement(new IntValue(context, i)).asStringValue());
            }
        } else if (args.length == 4 && desc[1].getDescriptor().equals("Ljava/lang/CharSequence;")) {
            // insert(int dstOffset, CharSequence s, int start, int end)
            StringValue s = objectChars(args[1]);
            if (s == null) return PlaceHolder.instance;
            IntValue start = args[2].asIntValue();
            IntValue end = args[3].asIntValue();
            chars = new StringValue(context, s.concrete.substring(start.concrete, end.concrete),
                    smgr.substring(s.formula, start.asIntegerFormula(),
                            imgr.subtract(end.asIntegerFormula(), start.asIntegerFormula())), -1);
        } else {
            chars = null;
        }
        if (chars == null) return PlaceHolder.instance;
        NumeralFormula.IntegerFormula off = offset.asIntegerFormula();
        String c = stringValue.concrete;
        setChars(c.substring(0, offset.concrete) + chars.concrete + c.substring(offset.concrete),
                smgr.concat(chars(imgr.makeNumber(0), off), chars.formula, chars(off, length())));
        return this;
    }

    private Value<?, ?> invokeReplace(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        // replace(int start, int end, String str); an end past the length replaces up to the end
        if (!(args[2] instanceof StringValue str)) return PlaceHolder.instance;
        IntValue start = args[0].asIntValue();
        IntValue end = args[1].asIntValue();
        NumeralFormula.IntegerFormula e = end.asIntegerFormula();
        setChars(new StringBuilder(stringValue.concrete).replace(start.concrete, end.concrete, str.concrete).toString(),
                smgr.concat(chars(imgr.makeNumber(0), start.asIntegerFormula()), str.formula,
                        smgr.substring(stringValue.formula, e, imgr.subtract(length(), e))));
        return this;
    }

    private Value<?, ?> invokeReverse() {
        // By the concrete length (pinned, see StringCallBranches): the chars in reverse order,
        // where Java then puts each surrogate pair (a low surrogate followed by a high one after
        // the reversal) back in order, scanning from the start.
        String reversed = new StringBuilder(stringValue.concrete).reverse().toString();
        if (!stringValue.isSymbolic()) {
            setChars(reversed, new Utf16(context.getFormulaManager()).chars(reversed));
            return this;
        }
        int n = stringValue.concrete.length();
        if (n > 256) return PlaceHolder.instance;
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        Utf16 chars = new Utf16(context.getFormulaManager());
        NumeralFormula.IntegerFormula[] r = new NumeralFormula.IntegerFormula[n];
        for (int i = 0; i < n; i++) r[i] = chars.codeAt(stringValue.formula, imgr.makeNumber(n - 1 - i));
        // swapped[i]: the pair at i, i + 1 is put back in order
        BooleanFormula[] swapped = new BooleanFormula[n];
        for (int i = 0; i < n; i++) {
            swapped[i] = i + 1 < n
                    ? bmgr.and(i > 0 ? bmgr.not(swapped[i - 1]) : bmgr.makeTrue(), chars.isLowSurrogate(r[i]), chars.isHighSurrogate(r[i + 1]))
                    : bmgr.makeFalse();
        }
        StringFormula result = smgr.makeString("");
        for (int i = 0; i < n; i++) {
            NumeralFormula.IntegerFormula c = r[i];
            if (i + 1 < n) c = bmgr.ifThenElse(swapped[i], r[i + 1], c);
            if (i > 0) c = bmgr.ifThenElse(swapped[i - 1], r[i - 1], c);
            result = smgr.concat(result, smgr.fromCodePoint(c));
        }
        setChars(reversed, result);
        return this;
    }

    private Value<?, ?> invokeSetCharAt(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        IntValue index = args[0].asIntValue();
        CharValue ch = args[1].asCharValue();
        NumeralFormula.IntegerFormula i = index.asIntegerFormula();
        StringBuilder sb = new StringBuilder(stringValue.concrete);
        sb.setCharAt(index.concrete, ch.concrete);
        setChars(sb.toString(), smgr.concat(chars(imgr.makeNumber(0), i), ch.asStringValue().formula,
                chars(imgr.add(i, imgr.makeNumber(1)), length())));
        return VoidValue.instance;
    }

    private Value<?, ?> invokeSetLength(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        // By the concrete length (pinned): the first n chars of the content padded with n '\0's.
        int n = args[0].asIntValue().concrete;
        StringBuilder sb = new StringBuilder(stringValue.concrete);
        sb.setLength(n);
        setChars(sb.toString(), smgr.substring(smgr.concat(stringValue.formula, smgr.makeString("\0".repeat(n))),
                imgr.makeNumber(0), imgr.makeNumber(n)));
        return VoidValue.instance;
    }

    private Value<?, ?> invokeSubstring(Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        // substring(start) is substring(start, length()); subSequence(start, end) is substring(start, end)
        if (args.length != 1 && args.length != 2) return PlaceHolder.instance;
        IntValue start = args[0].asIntValue();
        IntValue end = args.length == 2 ? args[1].asIntValue()
                : new IntValue(context, stringValue.concrete.length(), length());
        return new StringValue(context, stringValue.concrete.substring(start.concrete, end.concrete),
                chars(start.asIntegerFormula(), end.asIntegerFormula()), -1);
    }

    @Override
    public boolean isSymbolic() {
        return stringValue != null && stringValue.isSymbolic();
    }

    @Override
    public boolean isSymbolic(Set<Value<?, ?>> visited) {
        return isSymbolic();
    }

    @Override
    public ImmutableSet<String> getSymbolicVariables() {
        return stringValue == null ? ImmutableSet.of() : stringValue.getSymbolicVariables();
    }

    @Override
    public StringValue asStringValue() throws NotImplementedException {
        if (unknown || stringValue == null) {
            throw new NotImplementedException("The chars of this StringBuilder are not known");
        }
        return this.stringValue;
    }

    @Override
    public String toString() {
        String formulaString = "";
        String concreteString = "";
        if (stringValue != null) {
            formulaString = null != stringValue.formula ? stringValue.formula.toString() : "";
            concreteString = null != stringValue.concrete ? stringValue.concrete : "";
        }

        if (formulaString.length() > Config.instance().getLoggingFormulaLength()) {
            formulaString =
                    formulaString.substring(0, Config.instance().getLoggingFormulaLength()) + "...";
        }

        return "L" + className + " @"
                + Integer.toHexString(address)
                + (unknown ? " (unknown)" : "")
                + " ("
                + concreteString
                + ", "
                + formulaString
                + ")";
    }
}
