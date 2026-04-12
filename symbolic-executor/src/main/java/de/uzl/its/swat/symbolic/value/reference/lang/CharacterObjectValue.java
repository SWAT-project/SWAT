package de.uzl.its.swat.symbolic.value.reference.lang;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

public class CharacterObjectValue extends BoxedValue<CharValue> {

    private final IntegerFormulaManager ifm;
    private final StringFormulaManager sfm;

    public CharacterObjectValue(SolverContext ctx, CharValue val, int address) {
        super(ctx, address);
        SWATAssert.enforce(val != null, "Cannot create CharacterObjectValue with null value!");
        this.val = val;
        this.ifm = ctx.getFormulaManager().getIntegerFormulaManager();
        this.sfm = ctx.getFormulaManager().getStringFormulaManager();
    }

    /**
     * Gets the bound of the primitive type
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        SWATAssert.enforce(val != null, "Cannot create bound for uninitialized value!");
        return val.getBounds(upper);
    }

    /**
     * Generates a symbolic identifier for the underlying primitive value.
     *
     * @param prefixOrIdx The prefix that will be used to generate the symbolic identifier
     * @return The symbolic identifier
     * @throws NullPointerException If the (primitive) value is not initialized, i.e. null. Should
     *     not happen
     */
    @Override
    public String MAKE_SYMBOLIC(String prefixOrIdx) throws NullPointerException {
        SWATAssert.enforce(val != null, "Cannot make uninitialized value symbolic!");
        return val.MAKE_SYMBOLIC(prefixOrIdx);
    }

    /**
     * Generates a symbolic identifier for the underlying primitive value.
     *
     * @return The symbolic identifier
     * @throws NullPointerException If the (primitive) value is not initialized, i.e. null. Should
     *     not happen
     */
    @Override
    public String MAKE_SYMBOLIC() throws NullPointerException {
        SWATAssert.enforce(val != null, "Cannot make uninitialized value symbolic!");
        return val.MAKE_SYMBOLIC();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof CharacterObjectValue other) {
            return (val.equals(other.val));
        } else if (o instanceof CharValue other) {
            return val.equals(other);
        } else {
            return false;
        }
    }

    @Override
    public CharValue asCharValue() {
        return val;
    }

    @Override
    public CharacterObjectValue asCharacterObjectValue() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(val);
    }


    /**
     * Handles method invocation for Java's <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html">Character</a>
     * (Java 17).
     *
     * @param name The name of the method that is called
     * @param desc The Type descriptions for all Arguments
     * @param args The Value's representing the arguments
     * @return The return Value of the Method, or a PlaceHolder::instance if the Method is not
     *     implemented or void should be returned
     */
    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        return switch (name) {
            case "<init>" -> invokeInit(args, desc);
            case "charValue" -> invokeCharValue(args, desc);
            case "byteValue" -> invokeByteValue(args, desc);
            case "shortValue" -> invokeShortValue(args, desc);
            case "intValue" -> invokeIntValue(args, desc);
            case "longValue" -> invokeLongValue(args, desc);
            case "floatValue" -> invokeFloatValue(args, desc);
            case "doubleValue" -> invokeDoubleValue(args, desc);
            case "toString" -> invokeToString(args, desc);
            case "hashCode" -> invokeHashCode(args, desc);
            case "equals" -> invokeEquals(args, desc);
            case "compareTo" -> invokeCompareTo(args, desc);
            case "describeConstable" -> invokeDescribeConstable(args, desc);
            case "resolveConstantDesc" -> invokeResolveConstantDesc(args, desc);
            default -> PlaceHolder.instance;
        };
    }

    /**
     * Invocation handling for the Character constructor <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#%3Cinit%3E(char)">Character(char)</a>.
     * Creates a new Character object from a primitive char value.
     *
     * @param args List of Values that correspond to the method arguments (should contain one CharValue)
     * @param desc Array of Type descriptions of the methods' signature
     * @return VoidValue
     */
    private Value<?, ?> invokeInit(Value<?, ?>[] args, Type[] desc) {
        if (args[0] instanceof CharValue cv) {
            this.val = new CharValue(context, cv.concrete, cv.formula);
            return VoidValue.instance;
        }
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the Character instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#charValue()">charValue</a>().
     * Returns the primitive char value of this Character object.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return The primitive CharValue
     */
    private Value<?, ?> invokeCharValue(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for charValue() method");
        return val;
    }

    /**
     * Invocation handling for byteValue().
     * Returns the value as a byte after narrowing primitive conversion.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return ByteValue representing the converted value
     */
    private Value<?, ?> invokeByteValue(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for byteValue() method");
        return val.asByteValue();
    }

    /**
     * Invocation handling for shortValue().
     * Returns the value as a short after narrowing primitive conversion.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return ShortValue representing the converted value
     */
    private Value<?, ?> invokeShortValue(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for shortValue() method");
        return val.asShortValue();
    }

    /**
     * Invocation handling for intValue().
     * Returns the value as an int after widening primitive conversion.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return IntValue representing the converted value
     */
    private Value<?, ?> invokeIntValue(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for intValue() method");
        return val.asIntValue();
    }

    /**
     * Invocation handling for longValue().
     * Returns the value as a long after widening primitive conversion.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return LongValue representing the converted value
     */
    private Value<?, ?> invokeLongValue(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for longValue() method");
        return val.asLongValue();
    }

    /**
     * Invocation handling for floatValue().
     * Returns the value as a float after widening primitive conversion.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return FloatValue representing the converted value
     */
    private Value<?, ?> invokeFloatValue(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for floatValue() method");
        return val.asFloatValue();
    }

    /**
     * Invocation handling for doubleValue().
     * Returns the value as a double after widening primitive conversion.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return DoubleValue representing the converted value
     */
    private Value<?, ?> invokeDoubleValue(Value<?, ?>[] args, Type[] desc) throws NotImplementedException, ValueConversionException {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for doubleValue() method");
        return val.asDoubleValue();
    }

    /**
     * Invocation handling for the Character instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#toString()">toString</a>().
     * Returns a String representation of this Character object.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return StringValue representing the string representation
     */
    private Value<?, ?> invokeToString(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for toString() method");
        String result = val.concrete.toString();

        // Convert char to string symbolically using fromCodePoint
        StringFormulaManager sfm = context.getFormulaManager().getStringFormulaManager();
        StringFormula stringFormula = sfm.fromCodePoint(val.asIntegerFormula());

        return new StringValue(context, result, stringFormula, -1);
    }

    /**
     * Invocation handling for the Character instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#hashCode()">hashCode</a>().
     * Returns a hash code for this Character object.
     * No symbolic benefit, so returns placeholder to let concrete execution handle it.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return PlaceHolder since no symbolic benefit
     */
    private Value<?, ?> invokeHashCode(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the Character instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#equals(java.lang.Object)">equals</a>().
     * Compares this Character object with another object for equality.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature
     * @return BooleanValue representing the equality result
     */
    private Value<?, ?> invokeEquals(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 1, "Wrong number of arguments for equals() method");
        Value<?, ?> other = args[0];
        try {
            CharValue otherChar = other.asCharValue();
            boolean result = val.concrete.equals(otherChar.concrete);
            BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
            return new BooleanValue(context, result, bvmgr.equal(val.formula, otherChar.formula));
        } catch (Exception ignored) {
            return PlaceHolder.instance; // If the other value is not a CharValue, return PlaceHolder
        }
    }

    /**
     * Invocation handling for the Character instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#compareTo(java.lang.Character)">compareTo</a>().
     * Compares this Character instance with another Character instance.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature
     * @return IntValue representing the comparison result (this - other)
     */
    private Value<?, ?> invokeCompareTo(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        if (args.length == 1) {
            CharacterObjectValue other = args[0].asCharacterObjectValue();
            int result = val.concrete.compareTo(other.val.concrete);

            // compareTo returns the actual difference: this - other
            // Zero-extend both to 32-bit (char is unsigned) and subtract
            BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
            BitvectorFormula thisExt = bvmgr.extend(val.formula, 16, false);  // zero extend (unsigned)
            BitvectorFormula otherExt = bvmgr.extend(other.val.formula, 16, false);
            BitvectorFormula symbolicResult = bvmgr.subtract(thisExt, otherExt);

            return new IntValue(context, result, symbolicResult);
        } else {
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handling for the Character instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#describeConstable()">describeConstable</a>().
     * Returns PlaceHolder since requires complex reflection-based functionality.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature
     * @return PlaceHolder since not symbolically implementable
     */
    private Value<?, ?> invokeDescribeConstable(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the Character instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Character.html#resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup)">resolveConstantDesc</a>().
     * Returns PlaceHolder since requires complex reflection-based functionality.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature
     * @return PlaceHolder since not symbolically implementable
     */
    private Value<?, ?> invokeResolveConstantDesc(Value<?, ?>[] args, Type[] desc) {
        return PlaceHolder.instance;
    }


    @Override
    public IntValue asIntValue() throws ValueConversionException  {
        return val.asIntValue();
    }




    @Override
    public String toString() {
        return "Ljava/lang/Character; @" + Integer.toHexString(address) + " -> " + val;
    }
}
