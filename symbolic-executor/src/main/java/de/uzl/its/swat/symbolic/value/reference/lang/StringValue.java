package de.uzl.its.swat.symbolic.value.reference.lang;

import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.symbolic.UFs.EqualsIgnoreCaseUF;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.array.CharArrayValue;
import java.util.Arrays;

import de.uzl.its.swat.thread.ThreadHandler;
import lombok.Getter;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

import static java.lang.Thread.currentThread;

public class StringValue extends ObjectValue<StringFormula, String> {
    @Getter
    private static final String symbolicPrefix = Type.getType(String.class).getInternalName();

    /**
     * The UFs the models of compareTo, trim and lastIndexOf introduce. Each is defined exactly by a
     * constraint on its arguments that is added to the trace (addDefinition): it has one value for
     * any arguments (trim's bounds of an all-blank string excepted, where only lo == hi matters),
     * so a constraint over them is as exact as one over the inputs.
     */
    public static final java.util.Set<String> EXACT_UFS = java.util.Set.of("commonPrefixLength", "trimStart", "trimEnd", "lastIndexOf");

    static final int REPLACE_COUNT = 10;
    private StringFormulaManager smgr;
    private IntegerFormulaManager imgr;

    /**
     * Flag indicating this string was explicitly created via new String() in user code,
     * as opposed to being de-interned by SWAT's NoCacheMethodAdapter.
     * When true, reference equality comparisons on this string may have different
     * semantics than the original code intended.
     */
    @Getter
    private boolean userDeInterned = false;

    public StringValue(SolverContext context, String concrete, int address) {
        super(context, address);
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = smgr.makeString(concrete);
    }

    public StringValue(SolverContext context, String concrete, StringFormula formula, int address) {
        super(context, address);
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Compares if two references are not identical
     *
     * @param o2 The other string
     * @return A BooleanFormula representing the result
     */
    @Override
    public BooleanFormula IF_ACMPNE(ObjectValue o2) {
        if (o2 instanceof StringValue s2) {
            BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
            BooleanFormula b = bmgr.not(smgr.equal(formula, s2.formula));
            return b;
        } else {
            return super.IF_ACMPNE(o2);
        }
    }

    /**
     * Compares if two references are identical
     *
     * @param o2 The other string
     * @return A BooleanFormula representing the result
     */
    @Override
    public BooleanFormula IF_ACMPEQ(ObjectValue o2) {
        if (o2 instanceof StringValue s2) {
            BooleanFormula b = smgr.equal(formula, s2.formula);
            return b;
        } else {
            return super.IF_ACMPEQ(o2);
        }
    }

    @Override
    public Object getConcrete() {
        return concrete;
    }

    public String MAKE_SYMBOLIC(String prefixOrIdx) {
        if (prefixOrIdx.matches("-?\\d+")) {
            // We assume a constructed idx was passed as it is a number
            initSymbolic(symbolicPrefix, prefixOrIdx);
        } else if (prefixOrIdx.matches(".*-?\\d+")) {
            // Its a list which already has prefix and idx
            initSymbolicWithoutIdx(prefixOrIdx);
        } else {
            // If it's not a number we assume prefix
            initSymbolic(prefixOrIdx);
        }
        formula = smgr.makeVariable(name);
        return name;
    }

    public String MAKE_SYMBOLIC() {
        initSymbolic(symbolicPrefix);
        formula = smgr.makeVariable(name);
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
        formula = smgr.makeVariable(name);
        return name;
    }

    /**
     * Creates a formula that asserts that this symbolic value is withing the bounds of this type
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        if (!upper) {
            return context.getFormulaManager().getBooleanFormulaManager().makeBoolean(true);
        }
        // SMT strings range over all Unicode code points, Java chars over 16 bits only. Without
        // this bound the solver may pick a character above U+FFFF, which charAt (the code point
        // truncated to 16 bits) treats as a different one: the next run then diverges.
        return smgr.in(formula, smgr.closure(smgr.range('\u0000', '\uffff')));
    }

    /**
     * Handles method invocation for Java's <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html">String</a>
     * (Java 16).
     *
     * @param name The name of the method that is called
     * @param desc The Type descriptions for all Arguments
     * @param args The Value's representing the arguments
     * @return The return Value of the Method, or a PlaceHolder::instance if the Method is not
     *     implemented or void should be returned
     */
    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args)
            throws NotImplementedException, ValueConversionException {
        return switch (name) {
            case "<init>" -> invokeInit(args, desc);
            case "charAt" -> invokeCharAt(args, desc);
            case "chars" -> invokeChars(args, desc);
            case "codePointAt" -> invokeCodePointAt(args, desc);
            case "codePointBefore" -> invokeCodePointBefore(args, desc);
            case "codePointCount" -> invokeCodePointCount(args, desc);
            case "codePoints" -> invokeCodePoints(args, desc);
            case "compareTo" -> invokeCompareTo(args, desc);
            case "compareToIgnoreCase" -> invokeCompareToIgnoreCase(args, desc);
            case "concat" -> invokeConcat(args, desc);
            case "contains" -> invokeContains(args, desc);
            case "contentEquals" -> invokeContentEquals(args, desc);
            case "describeConstable" -> invokeDescribeConstable(args, desc);
            case "endsWith" -> invokeEndsWith(args, desc);
            case "equals" -> invokeEquals(args, desc);
            case "equalsIgnoreCase" -> invokeEqualsIgnoreCase(args, desc);
            case "formatted" -> invokeFormatted(args, desc);
            case "getBytes" -> invokeGetBytes(args, desc);
            case "getChars" -> invokeGetChars(args, desc);
            case "hashCode" -> invokeHashCode(args, desc);
            case "indent" -> invokeIndent(args, desc);
            case "indexOf" -> invokeIndexOf(args, desc);
            case "intern" -> invokeIntern(args, desc);
            case "isBlank" -> invokeIsBlank(args, desc);
            case "isEmpty" -> invokeIsEmpty(args, desc);
            case "lastIndexOf" -> invokeLastIndexOf(args, desc);
            case "length" -> invokeLength(args, desc);
            case "lines" -> invokeLines(args, desc);
            case "matches" -> invokeMatches(args, desc);
            case "offsetByCodePoints" -> invokeOffsetByCodePoints(args, desc);
            case "regionMatches" -> invokeRegionMatches(args, desc);
            case "repeat" -> invokeRepeat(args, desc);
            case "replace" -> invokeReplace(args, desc);
            case "replaceAll" -> invokeReplaceAll(args, desc);
            case "replaceFirst" -> invokeReplaceFirst(args, desc);
            case "resolveConstantDesc" -> invokeResolveConstantDesc(args, desc);
            case "split" -> invokeSplit(args, desc);
            case "startsWith" -> invokeStartsWith(args, desc);
            case "strip" -> invokeStrip(args, desc);
            case "stripIndent" -> invokeStripIndent(args, desc);
            case "stripLeading" -> invokeStripLeading(args, desc);
            case "stripTrailing" -> invokeStripTrailing(args, desc);
            case "subSequence" -> invokeSubSequence(args, desc);
            case "substring" -> invokeSubstring(args, desc);
            case "toCharArray" -> invokeToCharArray(args, desc);
            case "toLowerCase" -> invokeToLowerCase(args, desc);
            case "toString" -> invokeToString(args, desc);
            case "toUpperCase" -> invokeToUpperCase(args, desc);
            case "transform" -> invokeTransform(args, desc);
            case "translateEscapes" -> invokeTranslateEscapes(args, desc);
            case "trim" -> invokeTrim(args, desc);
            default -> PlaceHolder.instance;
        };
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Init">Init</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeInit(Value<?, ?>[] args, Type[] desc) {
        // Mark this string as user-de-interned since it was created via new String()
        // in user code (as opposed to SWAT's NoCacheMethodAdapter de-interning)
        this.userDeInterned = true;
        StringValue chars;
        if (args.length == 0) {
            chars = new StringValue(context, "", -1);
        } else if (args.length == 1 && args[0] instanceof CharArrayValue array) {
            // The concrete number of chars, which the call's branches pin (StringCallBranches).
            chars = array.asStringValue();
        } else if (args.length == 1) {
            // new String(String), new String(StringBuilder)
            chars = charsOf(args[0]);
        } else {
            chars = null;
        }
        if (chars == null) return PlaceHolder.instance;
        this.concrete = chars.concrete;
        this.formula = chars.formula;
        return VoidValue.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#CharAt">CharAt</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeCharAt(Value<?, ?>[] args, Type[] desc)
            throws NotImplementedException, ValueConversionException {
        if (args.length == 1) {
            IntValue index = args[0].asIntValue();
            char result = concrete.charAt(index.concrete);
            return new CharValue(context, result, smgr.charAt(formula, index.asIntegerFormula()));
        } else {
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Chars">Chars</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeChars(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#CodePointAt">CodePointAt</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeCodePointAt(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#CodePointBefore">CodePointBefore</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeCodePointBefore(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#CodePointCount">CodePointCount</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeCodePointCount(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#CodePoints">CodePoints</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeCodePoints(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#CompareTo">CompareTo</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeCompareTo(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        if (args.length != 1 || !(args[0] instanceof StringValue other)) return PlaceHolder.instance;
        int result = concrete.compareTo(other.concrete);
        if (!isSymbolic() && !other.isSymbolic()) return new IntValue(context, result);
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        NumeralFormula.IntegerFormula len = smgr.length(formula);
        NumeralFormula.IntegerFormula otherLen = smgr.length(other.formula);
        // k = the length of the longest common prefix, defined by a UF of both strings: the
        // prefixes up to k agree, and k is the end of one string or the chars at k differ.
        FunctionDeclaration<NumeralFormula.IntegerFormula> lcp = context.getFormulaManager().getUFManager()
                .declareUF("commonPrefixLength", FormulaType.IntegerType, FormulaType.StringType, FormulaType.StringType);
        NumeralFormula.IntegerFormula k = context.getFormulaManager().getUFManager().callUF(lcp, formula, other.formula);
        BooleanFormula kInsideBoth = bmgr.and(imgr.lessThan(k, len), imgr.lessThan(k, otherLen));
        addDefinition(bmgr.and(
                imgr.greaterOrEquals(k, imgr.makeNumber(0)),
                imgr.lessOrEquals(k, len),
                imgr.lessOrEquals(k, otherLen),
                smgr.equal(smgr.substring(formula, imgr.makeNumber(0), k), smgr.substring(other.formula, imgr.makeNumber(0), k)),
                bmgr.implication(kInsideBoth, bmgr.not(smgr.equal(smgr.charAt(formula, k), smgr.charAt(other.formula, k))))));
        // Java: the difference of the first differing chars, or else of the lengths.
        NumeralFormula.IntegerFormula cmp = bmgr.ifThenElse(kInsideBoth,
                imgr.subtract(smgr.toCodePoint(smgr.charAt(formula, k)), smgr.toCodePoint(smgr.charAt(other.formula, k))),
                imgr.subtract(len, otherLen));
        return new IntValue(context, result, cmp);
    }

    /** Records that a model is not exact for this call, so an exhausted search proves nothing. */
    static void recordPrecisionLoss() {
        try {
            ThreadHandler.getSymbolicTraceHandler(currentThread().getId()).recordSymbolicPrecisionLoss();
        } catch (Exception e) {
            throw new IllegalStateException("No symbolic trace handler for the current thread", e);
        }
    }

    /**
     * The chars of a CharSequence SWAT tracks: a String or a StringBuilder's content, else null.
     */
    public static StringValue charsOf(Value<?, ?> sequence) {
        if (sequence instanceof StringValue str) return str;
        if (sequence instanceof de.uzl.its.swat.symbolic.value.reference.StringBuilderValue sb) return sb.getContent();
        return null;
    }

    /** Adds a constraint that defines the UFs a model introduced; it holds on every path and is never negated. */
    private static void addDefinition(BooleanFormula definition) {
        try {
            ThreadHandler.getSymbolicTraceHandler(currentThread().getId()).addConstraint(definition);
        } catch (Exception e) {
            throw new IllegalStateException("No symbolic trace handler for the current thread", e);
        }
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#CompareToIgnoreCase">CompareToIgnoreCase</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeCompareToIgnoreCase(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Concat">Concat</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeConcat(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        if (args.length == 1) {
            StringValue str = args[0].asStringValue();
            String result = concrete.concat(str.concrete);
            return new StringValue(context, result, smgr.concat(formula, str.formula), -1);
        } else {
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Contains">Contains</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeContains(Value<?, ?>[] args, Type[] desc) {
        StringValue s = args.length == 1 ? charsOf(args[0]) : null;
        if (s != null) {
            boolean result = concrete.contains(s.concrete);
            return new BooleanValue(context, result, smgr.contains(formula, s.formula));
        } else {
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#ContentEquals">ContentEquals</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeContentEquals(Value<?, ?>[] args, Type[] desc) {
        StringValue other = args.length == 1 ? charsOf(args[0]) : null;
        if (other == null) return PlaceHolder.instance;
        return new BooleanValue(context, concrete.contentEquals(other.concrete), smgr.equal(formula, other.formula));
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#DescribeConstable">DescribeConstable</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeDescribeConstable(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#EndsWith">EndsWith</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeEndsWith(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        if (args.length == 1) {
            StringValue suffix = args[0].asStringValue();
            boolean result = concrete.endsWith(suffix.concrete);
            return new BooleanValue(context, result, smgr.suffix(suffix.formula, formula));
        } else {
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Equals">Equals</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeEquals(Value<?, ?>[] args, Type[] desc) {
        if (args.length == 1) {
            // Only a String is equal to a String (not, e.g., a StringBuilder with the same chars).
            if (args[0] instanceof StringValue str) {
                boolean result = concrete.equals(str.concrete);
                return new BooleanValue(context, result, smgr.equal(formula, str.formula));
            } else if (args[0] instanceof ObjectValue<?, ?> obj && obj.getAddress() == ObjectValue.ADDRESS_NULL) {
                return new BooleanValue(context, false);
            }
            return PlaceHolder.instance;
        } else {
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#EqualsIgnoreCase">EqualsIgnoreCase</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeEqualsIgnoreCase(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        SWATAssert.enforce(args.length == 1,
                "Wrong number of arguments for String.equalsIgnoreCase");

        try {
            StringValue rhs = args[0].asStringValue();

            // fast path if both concrete
            if (!this.isSymbolic() && !rhs.isSymbolic()) {
                return new BooleanValue(context, this.concrete.equalsIgnoreCase(rhs.concrete));
            }

            EqualsIgnoreCaseUF uf = ThreadHandler.getUFHandler(currentThread().getId()).getEqualsIgnoreCaseUF();

            // (1) the symbolic boolean you'll return on the stack
            BooleanFormula ufCall = uf.getUFCall(this.formula, rhs.formula);

            // (2) "general" constraints that define the UF (never negated)
            BooleanFormula constraints = uf.createEqualsIgnoreCaseConstraints(
                    this.formula, rhs.formula,
                    this.concrete, rhs.concrete,
                    this.isSymbolic(), rhs.isSymbolic());
            ThreadHandler.getSymbolicTraceHandler(currentThread().getId()).addConstraint(constraints);

            // also carry the concrete result for concolic steering
            boolean concreteResult = this.concrete.equalsIgnoreCase(rhs.concrete);
            return new BooleanValue(context, concreteResult, ufCall);

        } catch (Exception e) {
            logger.error("Error invoking equalsIgnoreCase on StringValue: {}", e.getMessage());
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Formatted">Formatted</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeFormatted(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#GetBytes">GetBytes</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeGetBytes(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#GetChars">GetChars</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeGetChars(Value<?, ?>[] args, Type[] desc) {
        if (args.length == 4) {
            try {
                IntValue srcBegin = args[0].asIntValue();
                IntValue srcEnd = args[1].asIntValue();
                CharArrayValue dst = args[2].asObjectValue().asArrayValue().asCharArrayValue();
                IntValue dstBegin = args[3].asIntValue();
                // By the concrete indices, which the call's branches pin (StringCallBranches).
                for (int idx = srcBegin.concrete; idx < srcEnd.concrete; idx++) {
                    Value<?, ?>[] charAtArgs = new Value[] { new IntValue(context, idx) };
                    Type[] charAtDesc = new Type[] { Type.INT_TYPE };
                    dst.storeElement(
                            new IntValue(context, dstBegin.concrete + idx - srcBegin.concrete),
                            (CharValue) invokeCharAt(charAtArgs, charAtDesc));
                }
                return VoidValue.instance;
            } catch (Exception ignored) {

                return PlaceHolder.instance;
            }

        } else {
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#HashCode">HashCode</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeHashCode(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Indent">Indent</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeIndent(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#IndexOf">IndexOf</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeIndexOf(Value<?, ?>[] args, Type[] desc)
            throws NotImplementedException, ValueConversionException {
        if (args.length == 1) {
            return switch (desc[0].getDescriptor()) {
                case "I" -> invokeIndexOf(args[0].asIntValue());
                case "Ljava/lang/String;" -> invokeIndexOf(args[0].asStringValue());
                default -> PlaceHolder.instance;
            };
        } else if (args.length == 2) {
            return switch (desc[0].getDescriptor()) {
                case "I" -> invokeIndexOf(args[0].asIntValue(), args[1].asIntValue());
                case "Ljava/lang/String;" -> invokeIndexOf(
                        args[0].asStringValue(), args[1].asIntValue());
                default -> PlaceHolder.instance;
            };
        } else {
            return PlaceHolder.instance;
        }
    }

    private Value<?, ?> invokeIndexOf(StringValue str) {
        return invokeIndexOf(str, new IntValue(context, 0));
    }

    private Value<?, ?> invokeIndexOf(StringValue str, IntValue fromIndex) {
        // Java clamps fromIndex into [0, length]; str.indexof returns -1 outside that range.
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        NumeralFormula.IntegerFormula from = fromIndex.asIntegerFormula();
        NumeralFormula.IntegerFormula length = smgr.length(formula);
        NumeralFormula.IntegerFormula clamped = bmgr.ifThenElse(imgr.lessThan(from, imgr.makeNumber(0)),
                imgr.makeNumber(0), bmgr.ifThenElse(imgr.greaterThan(from, length), length, from));
        return new IntValue(
                context,
                concrete.indexOf(str.concrete, fromIndex.concrete),
                smgr.indexOf(formula, str.formula, clamped));
    }

    private Value<?, ?> invokeIndexOf(IntValue ch) {
        return invokeIndexOf(ch, new IntValue(context, 0));
    }

    private Value<?, ?> invokeIndexOf(IntValue ch, IntValue fromIndex) {
        int result = concrete.indexOf(ch.concrete, fromIndex.concrete);
        if (!isSymbolic() && !ch.isSymbolic() && !fromIndex.isSymbolic()) return new IntValue(context, result);
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        IntValue index = (IntValue) invokeIndexOf(new StringValue(context, "", codePointString(ch), -1), fromIndex);
        // No code point, no match (as Java, which searches for a code point, not for its digits).
        return new IntValue(context, result, bmgr.ifThenElse(isCodePoint(ch), index.asIntegerFormula(), imgr.makeNumber(-1)));
    }

    /** Whether ch is a code point, i.e. in [0, 0x10FFFF]: the chars indexOf(int)/lastIndexOf(int) can find. */
    private BooleanFormula isCodePoint(IntValue ch) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        if (!ch.isSymbolic()) return bmgr.makeBoolean(Character.isValidCodePoint(ch.concrete));
        NumeralFormula.IntegerFormula c = ch.asIntegerFormula();
        return bmgr.and(imgr.greaterOrEquals(c, imgr.makeNumber(0)),
                imgr.lessOrEquals(c, imgr.makeNumber(Character.MAX_CODE_POINT)));
    }

    /**
     * The chars of code point ch in a Java string (String.valueOf(Character.toChars(ch))): the char
     * itself in the BMP, else its surrogate pair. Meaningful only if {@link #isCodePoint} holds.
     */
    public StringFormula codePointString(IntValue ch) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        if (!ch.isSymbolic()) {
            // Char by char: makeString would take a surrogate pair for one (SMT) character.
            if (!Character.isValidCodePoint(ch.concrete)) return smgr.makeString("");
            StringFormula chars = smgr.makeString("");
            for (char c : Character.toChars(ch.concrete)) chars = smgr.concat(chars, smgr.fromCodePoint(imgr.makeNumber(c)));
            return chars;
        }
        NumeralFormula.IntegerFormula c = ch.asIntegerFormula();
        NumeralFormula.IntegerFormula offset = imgr.subtract(c, imgr.makeNumber(Character.MIN_SUPPLEMENTARY_CODE_POINT));
        NumeralFormula.IntegerFormula high = imgr.add(imgr.makeNumber(Character.MIN_HIGH_SURROGATE), imgr.divide(offset, imgr.makeNumber(1024)));
        NumeralFormula.IntegerFormula low = imgr.add(imgr.makeNumber(Character.MIN_LOW_SURROGATE), imgr.modulo(offset, imgr.makeNumber(1024)));
        return bmgr.ifThenElse(imgr.lessThan(c, imgr.makeNumber(Character.MIN_SUPPLEMENTARY_CODE_POINT)),
                smgr.fromCodePoint(c),
                smgr.concat(smgr.fromCodePoint(high), smgr.fromCodePoint(low)));
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Intern">Intern</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeIntern(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#IsBlank">IsBlank</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeIsBlank(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#IsEmpty">IsEmpty</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeIsEmpty(Value<?, ?>[] args, Type[] desc) {
        return new BooleanValue(context, concrete.length() == 0, imgr.equal(smgr.length(formula), imgr.makeNumber(0)));
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#LastIndexOf">LastIndexOf</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeLastIndexOf(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        if (args.length < 1 || args.length > 2) return PlaceHolder.instance;
        IntValue from = args.length == 2 ? args[1].asIntValue() : new IntValue(context, Integer.MAX_VALUE);
        StringValue target;
        IntValue ch = null;
        int result;
        if (desc[0].getSort() == Type.INT) {
            // The chars of the code point; for anything else there is no match. By the concrete
            // char, which the call's branches pin (StringCallBranches): with a symbolic one the
            // window encoding below is too hard for the solver.
            ch = new IntValue(context, args[0].asIntValue().concrete);
            target = new StringValue(context, "", codePointString(ch), -1);
            result = concrete.lastIndexOf(ch.concrete, from.concrete);
        } else if (args[0] instanceof StringValue str) {
            target = str;
            result = concrete.lastIndexOf(str.concrete, from.concrete);
        } else {
            return PlaceHolder.instance;
        }
        if (!isSymbolic() && !target.isSymbolic() && !from.isSymbolic()) return new IntValue(context, result);
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        UFManager ufmgr = context.getFormulaManager().getUFManager();
        NumeralFormula.IntegerFormula tlen = smgr.length(target.formula);
        NumeralFormula.IntegerFormula fromF = from.asIntegerFormula();
        // The last start position a match may have: min(from, length - target length).
        NumeralFormula.IntegerFormula maxStart = imgr.subtract(smgr.length(formula), tlen);
        NumeralFormula.IntegerFormula last = bmgr.ifThenElse(imgr.lessThan(fromF, maxStart), fromF, maxStart);
        FunctionDeclaration<NumeralFormula.IntegerFormula> uf = ufmgr.declareUF("lastIndexOf", FormulaType.IntegerType,
                FormulaType.StringType, FormulaType.StringType, FormulaType.IntegerType);
        NumeralFormula.IntegerFormula r = ufmgr.callUF(uf, formula, target.formula, last);
        // Matches starting at or before `last` are the occurrences of target in the window s[0, last + tlen).
        // (Encoded with contains rather than indexOf, which Z3 solves much faster.)
        StringFormula window = smgr.substring(formula, imgr.makeNumber(0), imgr.add(last, tlen));
        StringFormula afterR = smgr.substring(window, imgr.add(r, imgr.makeNumber(1)),
                imgr.subtract(smgr.length(window), imgr.add(r, imgr.makeNumber(1))));
        BooleanFormula noMatch = bmgr.or(imgr.lessThan(last, imgr.makeNumber(0)), bmgr.not(smgr.contains(window, target.formula)));
        // r is -1 without a match; `last` for an empty target; otherwise a match with none after it in the window.
        addDefinition(bmgr.ifThenElse(noMatch,
                imgr.equal(r, imgr.makeNumber(-1)),
                bmgr.ifThenElse(imgr.equal(tlen, imgr.makeNumber(0)),
                        imgr.equal(r, last),
                        bmgr.and(imgr.greaterOrEquals(r, imgr.makeNumber(0)), imgr.lessOrEquals(r, last),
                                smgr.equal(smgr.substring(formula, r, tlen), target.formula),
                                bmgr.not(smgr.contains(afterR, target.formula))))));
        return new IntValue(context, result, ch == null ? r : bmgr.ifThenElse(isCodePoint(ch), r, imgr.makeNumber(-1)));
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Length">Length</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeLength(Value<?, ?>[] args, Type[] desc) {
        return new IntValue(context, concrete.length(), smgr.length(formula));
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Lines">Lines</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeLines(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Matches">Matches</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeMatches(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#OffsetByCodePoints">OffsetByCodePoints</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeOffsetByCodePoints(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#RegionMatches">RegionMatches</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeRegionMatches(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        int i = 0;
        if (args.length == 5) {
            // regionMatches(ignoreCase, ...): only the case-sensitive comparison is modelled.
            BooleanValue ignoreCase = args[0].asBooleanValue();
            if (ignoreCase.isSymbolic() || ignoreCase.concrete) return PlaceHolder.instance;
            i = 1;
        } else if (args.length != 4) {
            return PlaceHolder.instance;
        }
        if (!(args[i + 1] instanceof StringValue other)) return PlaceHolder.instance;
        IntValue toffset = args[i].asIntValue();
        IntValue ooffset = args[i + 2].asIntValue();
        IntValue len = args[i + 3].asIntValue();
        boolean result = concrete.regionMatches(toffset.concrete, other.concrete, ooffset.concrete, len.concrete);
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        NumeralFormula.IntegerFormula t = toffset.asIntegerFormula();
        NumeralFormula.IntegerFormula o = ooffset.asIntegerFormula();
        NumeralFormula.IntegerFormula l = len.asIntegerFormula();
        NumeralFormula.IntegerFormula zero = imgr.makeNumber(0);
        // As String.regionMatches: false if either region starts before 0 or ends past its string,
        // else true for an empty region, else the regions are compared.
        BooleanFormula inBounds = bmgr.and(imgr.greaterOrEquals(t, zero), imgr.greaterOrEquals(o, zero),
                imgr.lessOrEquals(t, imgr.subtract(smgr.length(formula), l)),
                imgr.lessOrEquals(o, imgr.subtract(smgr.length(other.formula), l)));
        BooleanFormula matches = bmgr.or(imgr.lessOrEquals(l, zero),
                smgr.equal(smgr.substring(formula, t, l), smgr.substring(other.formula, o, l)));
        return new BooleanValue(context, result, bmgr.and(inBounds, matches));
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Repeat">Repeat</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeRepeat(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Replace">Replace</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeReplace(Value<?, ?>[] args, Type[] desc)
            throws NotImplementedException, ValueConversionException {
        if (args.length == 2) {
            return switch (desc[0].getDescriptor()) {
                case "C" -> invokeReplace(args[0].asCharValue(), args[1].asCharValue());
                case "Ljava/lang/CharSequence;" -> invokeReplace(
                        args[0].asObjectValue(), args[1].asObjectValue());
                default -> PlaceHolder.instance;
            };
        }
        return PlaceHolder.instance;
    }

    private Value<?, ?> invokeReplace(ObjectValue<?, ?> target, ObjectValue<?, ?> replacement) {
        StringValue oldString = charsOf(target);
        StringValue newString = charsOf(replacement);
        if (oldString == null || newString == null) return PlaceHolder.instance;
        return replaceAll(oldString, newString);
    }

    private Value<?, ?> invokeReplace(CharValue oldChar, CharValue newChar) {
        return replaceAll(oldChar.asStringValue(), newChar.asStringValue());
    }

    /**
     * String.replace: every occurrence of target, left to right. Z3 hardly decides str.replace_all
     * (with a concrete target it gave up or hung where this did not), so only the first
     * REPLACE_COUNT occurrences are replaced, one by one, which is exact only for strings with as
     * many occurrences at most (and str.replace differs from Java for an empty target): precision
     * loss.
     */
    private StringValue replaceAll(StringValue target, StringValue replacement) {
        String newConcrete = this.concrete.replace(target.concrete, replacement.concrete);
        if (!isSymbolic() && !target.isSymbolic() && !replacement.isSymbolic()) {
            return new StringValue(context, newConcrete, ObjectValue.ADDRESS_UNKNOWN);
        }
        recordPrecisionLoss();
        StringFormula newFormula = formula;
        for (int i = 0; i < REPLACE_COUNT; i++) {
            newFormula = smgr.replace(newFormula, target.formula, replacement.formula);
        }
        return new StringValue(context, newConcrete, newFormula, ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#ReplaceAll">ReplaceAll</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeReplaceAll(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#ReplaceFirst">ReplaceFirst</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeReplaceFirst(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#ResolveConstantDesc">ResolveConstantDesc</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeResolveConstantDesc(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Split">Split</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeSplit(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#StartsWith">StartsWith</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeStartsWith(Value<?, ?>[] args, Type[] desc)
            throws NotImplementedException, ValueConversionException {
        if (args.length == 1) {
            return invokeStartsWith(args[0].asStringValue());
        } else if (args.length == 2) {
            return invokeStartsWith(args[0].asStringValue(), args[1].asIntValue());
        } else {
            return PlaceHolder.instance;
        }
    }

    private Value<?, ?> invokeStartsWith(StringValue prefix) {
        return new BooleanValue(
                context,
                concrete.startsWith(prefix.concrete),
                smgr.prefix(prefix.formula, formula));
    }

    private Value<?, ?> invokeStartsWith(StringValue prefix, IntValue toffset) {
        // As Java: false unless the prefix fits at toffset, i.e. 0 <= toffset <= length - prefix length.
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        NumeralFormula.IntegerFormula t = toffset.asIntegerFormula();
        NumeralFormula.IntegerFormula plen = smgr.length(prefix.formula);
        return new BooleanValue(
                context,
                concrete.startsWith(prefix.concrete, toffset.concrete),
                bmgr.and(
                        imgr.greaterOrEquals(t, imgr.makeNumber(0)),
                        imgr.lessOrEquals(t, imgr.subtract(smgr.length(formula), plen)),
                        smgr.equal(prefix.formula, smgr.substring(formula, t, plen))));
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Strip">Strip</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeStrip(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#StripIndent">StripIndent</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeStripIndent(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#StripLeading">StripLeading</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeStripLeading(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#StripTrailing">StripTrailing</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeStripTrailing(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#SubSequence">SubSequence</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeSubSequence(Value<?, ?>[] args, Type[] desc)
            throws NotImplementedException, ValueConversionException {
        // subSequence(begin, end) is substring(begin, end)
        return args.length == 2 ? invokeSubstring(args, desc) : PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Substring">Substring</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeSubstring(Value<?, ?>[] args, Type[] desc)
            throws NotImplementedException, ValueConversionException {
        if (args.length == 1) {
            return invokeSubstring(args[0].asIntValue());
        } else if (args.length == 2) {
            return invokeSubstring(args[0].asIntValue(), args[1].asIntValue());
        } else {
            return PlaceHolder.instance;
        }
    }

    private Value<?, ?> invokeSubstring(IntValue beginIndex) {
        IntValue endIndex = new IntValue(context, concrete.length(), smgr.length(this.formula));
        return invokeSubstring(beginIndex, endIndex);
    }

    private Value<?, ?> invokeSubstring(IntValue beginIndex, IntValue endIndex) {
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        return new StringValue(
                context,
                concrete.substring(beginIndex.concrete, endIndex.concrete),
                smgr.substring(
                        formula,
                        beginIndex.asIntegerFormula(),
                        imgr.subtract(endIndex.asIntegerFormula(), beginIndex.asIntegerFormula())),
                -1);
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#ToCharArray">ToCharArray</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeToCharArray(Value<?, ?>[] args, Type[] desc)
            throws NotImplementedException, ValueConversionException {

        // As long as the concrete string, which the call's branches pin (StringCallBranches).
        CharArrayValue dst = new CharArrayValue(context, new IntValue(context, concrete.length()), -1);
        for (int idx = 0; idx < concrete.length(); idx++) {
            Value<?, ?>[] charAtArgs = new Value[] { new IntValue(context, idx) };
            Type[] charAtDesc = new Type[] { Type.INT_TYPE };
            dst.storeElement(
                    new IntValue(context, idx),
                    (CharValue) invokeCharAt(charAtArgs, charAtDesc));
        }
        return dst;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#ToLowerCase">ToLowerCase</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeToLowerCase(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#ToString">ToString</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeToString(Value<?, ?>[] args, Type[] desc) {
        return this;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#ToUpperCase">ToUpperCase</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeToUpperCase(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Transform">Transform</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeTransform(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#TranslateEscapes">TranslateEscapes</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeTranslateEscapes(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Trim">Trim</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeTrim(Value<?, ?>[] args, Type[] desc) {
        if (args.length != 0) return PlaceHolder.instance;
        String result = concrete.trim();
        if (!isSymbolic()) return new StringValue(context, result, -1);
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        UFManager ufmgr = context.getFormulaManager().getUFManager();
        // trim() keeps [lo, hi): lo and hi are UFs of the string, defined by what they cut off
        // being made of chars <= ' ' only, and by the kept part, if any, not starting or ending in one.
        FunctionDeclaration<NumeralFormula.IntegerFormula> loUF = ufmgr.declareUF("trimStart", FormulaType.IntegerType, FormulaType.StringType);
        FunctionDeclaration<NumeralFormula.IntegerFormula> hiUF = ufmgr.declareUF("trimEnd", FormulaType.IntegerType, FormulaType.StringType);
        NumeralFormula.IntegerFormula lo = ufmgr.callUF(loUF, formula);
        NumeralFormula.IntegerFormula hi = ufmgr.callUF(hiUF, formula);
        NumeralFormula.IntegerFormula len = smgr.length(formula);
        RegexFormula trimmed = smgr.range((char) 0, ' ');
        addDefinition(bmgr.and(
                imgr.greaterOrEquals(lo, imgr.makeNumber(0)),
                imgr.lessOrEquals(lo, hi),
                imgr.lessOrEquals(hi, len),
                smgr.in(smgr.substring(formula, imgr.makeNumber(0), lo), smgr.closure(trimmed)),
                smgr.in(smgr.substring(formula, hi, imgr.subtract(len, hi)), smgr.closure(trimmed)),
                bmgr.implication(imgr.lessThan(lo, hi), bmgr.and(
                        bmgr.not(smgr.in(smgr.charAt(formula, lo), trimmed)),
                        bmgr.not(smgr.in(smgr.charAt(formula, imgr.subtract(hi, imgr.makeNumber(1))), trimmed))))));
        return new StringValue(context, result, smgr.substring(formula, lo, imgr.subtract(hi, lo)), -1);
    }

    @Override
    public String getConcreteEncoded() {
        char[] chars = concrete.toCharArray();
        int[] tmp = new int[chars.length];
        for (int i = 0; i < chars.length; i++) {
            tmp[i] = chars[i];
        }
        return Arrays.toString(tmp);
    }

    @Override
    public StringValue asStringValue() {
        return this;
    }

    @Override
    public StringValue asObjectValue() {
        return this;
    }

    @Override
    public boolean isSymbolic() {
        return !context.getFormulaManager().extractVariables(formula).isEmpty();
    }

    @Override
    public ImmutableSet<String> getSymbolicVariables() {
        return context.getFormulaManager().extractVariables(formula).keySet();
    }

    @Override
    public String getSymPrefix() {
        return symbolicPrefix;
    }

    @Override
    public String toString() {
        String formulaString = null != formula ? this.formula.toString() : "";
        String concreteString = null != concrete ? concrete : "";

        if (formulaString.length() > Config.instance().getLoggingFormulaLength()) {
            formulaString = formulaString.substring(0, Config.instance().getLoggingFormulaLength()) + "...";
        }

        return "StringValue[" + this.internalID + "]; @"
                + Integer.toHexString(address)
                + " ("
                + concreteString
                + ", "
                + formulaString
                + ")";
    }
}
