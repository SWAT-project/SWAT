package de.uzl.its.symbolic.value.reference;

import de.uzl.its.swat.config.Config;
import de.uzl.its.symbolic.value.PlaceHolder;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.VoidValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.array.CharArrayValue;
import de.uzl.its.symbolic.value.reference.lang.StringValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

public final class StringBuilderValue extends ObjectValue<Object, Object> {
    private final StringFormulaManager smgr;
    private final IntegerFormulaManager imgr;
    private StringValue stringValue;
    private IntValue capacity;

    public StringBuilderValue(SolverContext context) {
        super(context, 100, -1);
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();

        this.stringValue = new StringValue(context, "", -1);
    }

    public StringBuilderValue(SolverContext context, StringValue v, int address) {
        super(context, 100, address);
        this.smgr = context.getFormulaManager().getStringFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.stringValue = v;
    }

    public Value<?, ?> getStringValue() {
        return stringValue;
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
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) {
        return switch (name) {
            case "<init>" -> invokeInit(args, desc);
            case "append" -> invokeAppend(args, desc);
            case "appendCodePoint" -> invokeAppendCodePoint(args, desc);
            case "capacity" -> invokeCapacity(args, desc);
            case "charAt" -> invokeCharAt(args, desc);
            case "chars" -> invokeChars(args, desc);
            case "codePointAt" -> invokeCodePointAt(args, desc);
            case "codePointBefore" -> invokeCodePointBefore(args, desc);
            case "codePointCount" -> invokeCodePointCount(args, desc);
            case "codePoints" -> invokeCodePoints(args, desc);
            case "compareTo" -> invokeCompareTo(args, desc);
            case "delete" -> invokeDelete(args, desc);
            case "deleteCharAt" -> invokeDeleteCharAt(args, desc);
            case "ensureCapacity" -> invokeEnsureCapacity(args, desc);
            case "getChars" -> invokeGetChars(args, desc);
            case "indexOf" -> invokeIndexOf(args, desc);
            case "insert" -> invokeInsert(args, desc);
            case "lastIndexOf" -> invokeLastIndexOf(args, desc);
            case "length" -> invokeLength(args, desc);
            case "offsetByCodePoints" -> invokeOffsetByCodePoints(args, desc);
            case "replace" -> invokeReplace(args, desc);
            case "reverse" -> invokeReverse(args, desc);
            case "setCharAt" -> invokeSetCharAt(args, desc);
            case "setLength" -> invokeSetLength(args, desc);
            case "subSequence" -> invokeSubSequence(args, desc);
            case "substring" -> invokeSubstring(args, desc);
            case "toString" -> invokeToString(args, desc);
            case "trimToSize" -> invokeTrimToSize(args, desc);
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
        int numberOfArgs = args.length;
        if (numberOfArgs == 0) {
            return invokeInit();
        } else if (numberOfArgs == 1) {
            return switch (desc[0].getDescriptor()) {
                case "I" -> invokeInit(args[0].asIntValue());
                case "Ljava/lang/String;" -> invokeInit(args[0].asStringValue());
                case "Ljava/lang/CharSequence;" -> invokeInit(args[0].asObjectValue());
                default -> PlaceHolder.instance;
            };
        }
        return PlaceHolder.instance;
    }

    private Value<?, ?> invokeInit() {
        this.capacity = new IntValue(this.context, 16);
        this.stringValue = new StringValue(this.context, "", -1);
        return VoidValue.instance;
    }

    private Value<?, ?> invokeInit(IntValue capacity) {
        this.capacity = capacity;
        this.stringValue = new StringValue(this.context, "", -1);
        return VoidValue.instance;
    }

    private Value<?, ?> invokeInit(@SuppressWarnings("unused") ObjectValue<?, ?> seq) {
        return PlaceHolder.instance;
    }

    private Value<?, ?> invokeInit(StringValue str) {
        this.stringValue = str;
        return VoidValue.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Append">Append</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeAppend(Value<?, ?>[] args, Type[] desc) {
        int numberOfArgs = args.length;
        if (numberOfArgs == 1) {
            return switch (desc[0].getDescriptor()) {
                case "I" -> invokeAppend(args[0].asIntValue().asStringValue());
                case "Z" -> invokeAppend(args[0].asBooleanValue().asStringValue());
                case "C" -> invokeAppend(args[0].asCharValue().asStringValue());
                case "D" -> invokeAppend(args[0].asDoubleValue().asStringValue());
                case "F" -> invokeAppend(args[0].asFloatValue().asStringValue());
                case "J" -> invokeAppend(args[0].asLongValue().asStringValue());
                case "Ljava/lang/String;" -> invokeAppend(args[0].asStringValue());
                case "Ljava/lang/CharSequence;",
                        "Ljava/lang/StringBuffer;",
                        "Ljava/lang/Object;" -> invokeAppend(args[0].asObjectValue());
                case "[C" -> invokeAppend(
                        args[0].asObjectValue().asArrayValue().asCharArrayValue());
                default -> PlaceHolder.instance;
            };
        } else if (numberOfArgs == 3) {
            return switch (desc[0].getDescriptor()) {
                case "[C" -> invokeAppend(
                        args[0].asObjectValue().asArrayValue().asCharArrayValue(),
                        args[1].asIntValue(),
                        args[2].asIntValue());
                case "Ljava/lang/CharSequence;" -> invokeAppend(
                        args[0].asObjectValue(), args[1].asIntValue(), args[2].asIntValue());
                default -> PlaceHolder.instance;
            };
        }
        return PlaceHolder.instance;
    }

    private StringBuilderValue invokeAppend(StringValue s) {
        String desc = "(Ljava.lang.String;)";
        Type[] type = Type.getArgumentTypes(desc);
        this.stringValue =
                (StringValue) stringValue.invokeMethod("concat", type, new StringValue[] {s});
        return this;
    }

    private Value<?, ?> invokeAppend(CharArrayValue str) {
        int size = str.size.concrete;
        for (int i = 0; i < size; i++) {
            invokeAppend(str.getElement(new IntValue(str.getContext(), i)).asStringValue());
        }

        return this;
    }

    private Value<?, ?> invokeAppend(@SuppressWarnings("unused") ObjectValue<?, ?> obj) {
        return PlaceHolder.instance;
    }

    private Value<?, ?> invokeAppend(ObjectValue<?, ?> obj, IntValue start, IntValue end) {
        try {
            StringValue s = obj.asStringValue();
            Type[] desc = new Type[] {Type.INT_TYPE, Type.INT_TYPE};
            Value<?, ?>[] args = new Value<?, ?>[] {start, end};
            StringValue substring = (StringValue) s.invokeMethod("substring", desc, args);
            return invokeAppend(substring);
        } catch (Exception ignored) {
            return PlaceHolder.instance;
        }
    }

    private Value<?, ?> invokeAppend(CharArrayValue str, IntValue offset, IntValue len) {
        for (int i = 0; i < len.concrete; i++) {
            invokeAppend(
                    str.getElement(new IntValue(str.getContext(), i + offset.concrete))
                            .asStringValue());
        }

        return this;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#AppendCodePoint">AppendCodePoint().</a>
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeAppendCodePoint(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Capacity">Capacity</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeCapacity(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        return capacity;
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
    private Value<?, ?> invokeCharAt(Value<?, ?>[] args, @SuppressWarnings("unused") Type[] desc) {
        return new CharValue(
                context,
                this.stringValue.concrete.charAt(args[0].asIntValue().concrete),
                this.smgr.charAt(this.stringValue.formula, args[0].asIntValue().formula));
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
    private Value<?, ?> invokeChars(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
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
    private Value<?, ?> invokeCodePointAt(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
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
    private Value<?, ?> invokeCodePointBefore(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
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
    private Value<?, ?> invokeCodePointCount(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
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
    private Value<?, ?> invokeCodePoints(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
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
    private Value<?, ?> invokeCompareTo(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Delete">Delete</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeDelete(Value<?, ?>[] args, @SuppressWarnings("unused") Type[] desc) {
        IntValue start = (IntValue) args[0];
        IntValue end = (IntValue) args[1];
        NumeralFormula.IntegerFormula remainingLength =
                this.imgr.subtract(this.smgr.length(this.stringValue.formula), end.formula);
        this.stringValue.formula =
                this.smgr.concat(
                        this.smgr.substring(
                                this.stringValue.formula,
                                new IntValue(context, 0).formula,
                                start.formula),
                        this.smgr.substring(
                                this.stringValue.formula, end.formula, remainingLength));
        StringBuilder deleteBuilder = new StringBuilder(this.stringValue.concrete);
        this.stringValue.concrete = deleteBuilder.delete(start.concrete, end.concrete).toString();
        return this;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#DeleteCharAt">DeleteCharAt</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeDeleteCharAt(
            Value<?, ?>[] args, @SuppressWarnings("unused") Type[] desc) {
        IntValue endIndex =
                new IntValue(
                        this.context,
                        args[0].asIntValue().concrete + 1,
                        this.imgr.add(
                                args[0].asIntValue().formula,
                                new IntValue(this.context, 1).formula));
        return invokeDelete(
                new Value<?, ?>[] {args[0], endIndex}, new Type[] {Type.INT_TYPE, Type.INT_TYPE});
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#EnsureCapacity">EnsureCapacity</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeEnsureCapacity(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
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
    private Value<?, ?> invokeGetChars(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
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
    private Value<?, ?> invokeIndexOf(Value<?, ?>[] args, @SuppressWarnings("unused") Type[] desc) {
        if (args.length == 1) {
            return invokeIndexOf(args[0].asStringValue());
        } else if (args.length == 2) {
            return invokeIndexOf(args[0].asStringValue(), args[1].asIntValue());
        }
        return PlaceHolder.instance;
    }

    private Value<?, ?> invokeIndexOf(StringValue str) {
        return new IntValue(
                this.context,
                this.stringValue.concrete.indexOf(str.concrete),
                this.smgr.indexOf(
                        this.stringValue.formula,
                        str.formula,
                        new IntValue(this.context, 0).formula));
    }

    private Value<?, ?> invokeIndexOf(StringValue str, IntValue fromIndex) {
        return new IntValue(
                this.context,
                this.stringValue.concrete.indexOf(str.concrete),
                this.smgr.indexOf(this.stringValue.formula, str.formula, fromIndex.formula));
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Insert">Insert()</a>.
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeInsert(Value<?, ?>[] args, Type[] desc) {
        if (args.length == 2) {
            return switch (desc[1].getDescriptor()) {
                case "I" -> invokeInsert(
                        args[0].asIntValue(), args[1].asIntValue().asStringValue());
                case "Z" -> invokeInsert(
                        args[0].asIntValue(), args[1].asBooleanValue().asStringValue());
                case "C" -> invokeInsert(
                        args[0].asIntValue(), args[1].asCharValue().asStringValue());
                case "D" -> invokeInsert(
                        args[0].asIntValue(), args[1].asDoubleValue().asStringValue());
                case "F" -> invokeInsert(
                        args[0].asIntValue(), args[1].asFloatValue().asStringValue());
                case "J" -> invokeInsert(
                        args[0].asIntValue(), args[1].asLongValue().asStringValue());
                case "Ljava/lang/String;" -> invokeInsert(
                        args[0].asIntValue(), args[1].asStringValue());
                case "Ljava/lang/CharSequence;", "Ljava/lang/Object;" -> invokeInsert(
                        args[0].asIntValue(), args[1].asObjectValue());
                case "[C" -> invokeInsert(
                        args[0].asIntValue(),
                        args[1].asObjectValue().asArrayValue().asCharArrayValue());
                default -> PlaceHolder.instance;
            };
        } else if (args.length == 4) {
            return switch (desc[1].getDescriptor()) {
                case "Ljava/lang/CharSequence;" -> invokeInsert(
                        args[0].asIntValue(),
                        args[1].asObjectValue(),
                        args[2].asIntValue(),
                        args[3].asIntValue());
                case "[C" -> invokeInsert(
                        args[0].asIntValue(),
                        args[1].asObjectValue().asArrayValue().asCharArrayValue(),
                        args[2].asIntValue(),
                        args[3].asIntValue());
                default -> PlaceHolder.instance;
            };
        }
        return PlaceHolder.instance;
    }

    private Value<?, ?> invokeInsert(IntValue offset, StringValue str) {
        this.stringValue.formula =
                this.smgr.concat(
                        smgr.substring(
                                this.stringValue.formula,
                                new IntValue(this.context, 0).formula,
                                offset.formula),
                        str.formula,
                        smgr.substring(
                                this.stringValue.formula,
                                offset.formula,
                                smgr.length(stringValue.formula)));
        this.stringValue.concrete =
                this.stringValue
                        .concrete
                        .substring(0, offset.concrete)
                        .concat(String.valueOf(str.concrete))
                        .concat(this.stringValue.concrete.substring(offset.concrete));
        return this;
    }

    private Value<?, ?> invokeInsert(
            @SuppressWarnings("unused") IntValue offset,
            @SuppressWarnings("unused") ObjectValue<?, ?> obj) {
        return PlaceHolder.instance;
    }

    private Value<?, ?> invokeInsert(IntValue offset, CharArrayValue str) {
        int size = str.size.concrete;
        StringValue newStr = new StringValue(this.context, "", -1);
        for (int i = 0; i < size; i++) {
            newStr.invokeMethod(
                    "concat",
                    new Type[] {Type.getType("Ljava/lang/String;")},
                    new Value[] {
                        str.getElement(new IntValue(str.getContext(), i)).asStringValue()
                    });
        }
        invokeInsert(offset, newStr);
        return this;
    }

    private Value<?, ?> invokeInsert(
            IntValue index, CharArrayValue str, IntValue offset, IntValue len) {
        StringValue newStr = new StringValue(this.context, "", -1);
        for (int i = 0; i < len.concrete; i++) {
            newStr.invokeMethod(
                    "concat",
                    new Type[] {Type.getType("Ljava/lang/String;")},
                    new Value[] {
                        str.getElement(new IntValue(str.getContext(), i + offset.concrete))
                                .asStringValue()
                    });
        }
        invokeInsert(index, newStr);
        return this;
    }

    private Value<?, ?> invokeInsert(
            @SuppressWarnings("unused") IntValue index,
            @SuppressWarnings("unused") ObjectValue<?, ?> str,
            @SuppressWarnings("unused") IntValue offset,
            @SuppressWarnings("unused") IntValue len) {
        return PlaceHolder.instance;
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
    private Value<?, ?> invokeLastIndexOf(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        return PlaceHolder.instance;
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
    private Value<?, ?> invokeLength(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        return new IntValue(
                this.context,
                this.stringValue.concrete.length(),
                this.smgr.length(this.stringValue.formula));
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
    private Value<?, ?> invokeOffsetByCodePoints(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
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
    private Value<?, ?> invokeReplace(Value<?, ?>[] args, @SuppressWarnings("unused") Type[] desc) {
        IntValue start = (IntValue) args[0];
        IntValue end = (IntValue) args[1];
        StringValue replacement = (StringValue) args[2];
        StringFormula substring =
                this.smgr.substring(
                        this.stringValue.formula,
                        start.formula,
                        this.imgr.subtract(end.formula, start.formula));
        this.stringValue.formula =
                smgr.replace(this.stringValue.formula, substring, replacement.formula);
        StringBuilder builderReplace = new StringBuilder(this.stringValue.concrete);
        this.stringValue.concrete =
                builderReplace
                        .replace(start.concrete, end.concrete, replacement.concrete)
                        .toString();
        return this;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#Reverse">Reverse</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeReverse(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#SetCharAt">SetCharAt</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeSetCharAt(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        /*
            IntValue index = args[0].asIntValue();
            CharValue newChar = args[1].asCharValue();

            NumeralFormula.IntegerFormula secondIndex = this.imgr.add(index.formula, new IntValue(this.context, 1).formula);
            NumeralFormula.IntegerFormula lengthOfSecondSubstring =
                    this.imgr.subtract(this.smgr.length(this.stringValue.formula), secondIndex);
            this.stringValue.formula = this.smgr.concat(
                    this.smgr.substring(this.stringValue.formula, new IntValue(this.context, 0).formula, index.formula),
                    newChar.formula,
                    this.smgr.substring(this.stringValue.formula, this.imgr.add(index.formula, secondIndex), lengthOfSecondSubstring));

            StringBuilder builderSetCharAt = new StringBuilder(this.stringValue.concrete);
            builderSetCharAt.setCharAt(index.concrete, newChar.concrete);
            this.stringValue.concrete = builderSetCharAt.toString();
        }
             */
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#SetLength">SetLength</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeSetLength(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        // ToDo this does not work! Null bytes need to be append to fill the sequence!
        /*
        IntValue newLength = args[0].asIntValue();
        this.stringValue.formula = this.smgr.substring(this.stringValue.formula, new IntValue(this.context, 0).formula, newLength.formula);
        // Does this behave correctly for newLength > length(concrete) and for newLength == 0?
        this.stringValue.concrete = this.stringValue.concrete.substring(0, newLength.concrete);

         */
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
    private Value<?, ?> invokeSubSequence(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        return PlaceHolder.instance;
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
    private Value<?, ?> invokeSubstring(
            Value<?, ?>[] args, @SuppressWarnings("unused") Type[] desc) {
        if (args.length == 1) {
            return invokeSubstring(args[0].asIntValue());
        } else if (args.length == 2) {
            return invokeSubstring(args[0].asIntValue(), args[1].asIntValue());
        }
        return PlaceHolder.instance;
    }

    private Value<?, ?> invokeSubstring(IntValue start) {
        NumeralFormula.IntegerFormula remainingLength =
                this.imgr.subtract(this.smgr.length(this.stringValue.formula), start.formula);
        this.stringValue.formula =
                this.smgr.substring(this.stringValue.formula, start.formula, remainingLength);
        this.stringValue.concrete = this.stringValue.concrete.substring(start.concrete);
        return new StringValue(
                this.context, this.stringValue.concrete, this.stringValue.formula, -1);
    }

    private Value<?, ?> invokeSubstring(IntValue start, IntValue end) {
        this.stringValue.formula =
                this.smgr.substring(this.stringValue.formula, start.formula, end.formula);
        this.stringValue.concrete =
                this.stringValue.concrete.substring(start.concrete, end.concrete);
        return new StringValue(
                this.context, this.stringValue.concrete, this.stringValue.formula, -1);
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
    private Value<?, ?> invokeToString(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        return new StringValue(
                this.context, this.stringValue.concrete, this.stringValue.formula, -1);
    }

    /**
     * Invocation handling for the String instance method <a
     * href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/lang/String.html#TrimToSize">TrimToSize</a>().
     * Returns PlaceHolder::instance if the method is not yet implemented, or the Value that
     * represents the result of the method including symbolic handling.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature. No guarantee is given that
     *     the Value in args is of the same type.
     * @return The resulting Value or PlaceHolder::instance
     */
    private Value<?, ?> invokeTrimToSize(
            @SuppressWarnings("unused") Value<?, ?>[] args,
            @SuppressWarnings("unused") Type[] desc) {
        return PlaceHolder.instance;
    }

    @Override
    public StringValue asStringValue() {
        return this.stringValue;
    }

    @Override
    public String toString() {
        String formulaString = "";
        String concreteString = "";
        if (stringValue != null){
            formulaString =  null != stringValue.formula ? stringValue.formula.toString(): "";
            concreteString = null != stringValue.concrete ? stringValue.concrete : "";
        }

        if (formulaString.length() > Config.instance().getFormulaPrintLength()) {
            formulaString = formulaString.substring(0, Config.instance().getFormulaPrintLength()) + "...";
        }

        return "Ljava/lang/StringBuilder @"
                + Integer.toHexString(address)
                + " ("
                + concreteString
                + ", "
                + formulaString
                + ")";
    }
}
