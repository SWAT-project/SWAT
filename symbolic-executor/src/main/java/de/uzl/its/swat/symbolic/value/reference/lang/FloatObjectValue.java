package de.uzl.its.swat.symbolic.value.reference.lang;

import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormulaManager;

import com.google.common.base.Objects;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;

public class FloatObjectValue extends BoxedValue<FloatValue> {

    private FloatingPointFormulaManager fpfm;
    private StringFormulaManager sfm;

    public FloatObjectValue(SolverContext ctx, FloatValue val, int address) {
        super(ctx, address);
        SWATAssert.enforce(val != null, "Float has to be instantiated with default value!");
        this.val = val;
        this.fpfm = ctx.getFormulaManager().getFloatingPointFormulaManager();
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
        // Todo: Could be relaxed (?)
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

    /**
     * Handles method invocation for Java's <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html">Float</a>
     * (Java 17).
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
            case "floatValue" -> invokeFloatValue(args, desc);
            case "doubleValue" -> invokeDoubleValue(args, desc);
            case "longValue" -> invokeLongValue(args, desc);
            case "intValue" -> invokeIntValue(args, desc);
            case "shortValue" -> invokeShortValue(args, desc);
            case "byteValue" -> invokeByteValue(args, desc);
            case "toString" -> invokeToString(args, desc);
            case "hashCode" -> invokeHashCode(args, desc);
            case "equals" -> invokeEquals(args, desc);
            case "compareTo" -> invokeCompareTo(args, desc);
            case "isNaN" -> invokeIsNaN(args, desc);
            case "isInfinite" -> invokeIsInfinite(args, desc);
            case "describeConstable" -> invokeDescribeConstable(args, desc);
            case "resolveConstantDesc" -> invokeResolveConstantDesc(args, desc);
            default -> PlaceHolder.instance;
        };
    }

    private Value<?, ?> invokeInit(Value<?, ?>[] args, Type[] desc) {
        if (args[0] instanceof FloatValue fv) {
            this.val = new FloatValue(context, fv.concrete, fv.formula);
            return VoidValue.instance;
        }
        return PlaceHolder.instance;
    }


    /**
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#floatValue()">floatValue</a>().
     * Returns the primitive float value of this Float object.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return The primitive FloatValue
     */
    private Value<?, ?> invokeFloatValue(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for floatValue() method");
        return val;
    }

    /**
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#doubleValue()">doubleValue</a>().
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
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#longValue()">longValue</a>().
     * Returns the value as a long after narrowing primitive conversion.
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
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#intValue()">intValue</a>().
     * Returns the value as an int after narrowing primitive conversion.
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
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#shortValue()">shortValue</a>().
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
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#byteValue()">byteValue</a>().
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
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#toString()">toString</a>().
     * Returns a String representation of this Float object.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return StringValue representing the string representation
     */
    private Value<?, ?> invokeToString(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for toString() method");

        // Symbolic converison currently not supported
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#hashCode()">hashCode</a>().
     * Returns a hash code for this Float object.
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
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#equals(java.lang.Object)">equals</a>().
     * Compares this Float object with another object for equality.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature
     * @return BooleanValue representing the equality result
     */
    private Value<?, ?> invokeEquals(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 1, "Wrong number of arguments for equals() method");
        Value<?, ?> other = args[0];
        try {
            FloatObjectValue otherFloat = other.asFloatObjectValue();
            boolean result = val.concrete.equals(otherFloat.val.concrete);
            return new BooleanValue(context, result, fpfm.equalWithFPSemantics(val.formula, otherFloat.val.formula));
        } catch (Exception ignored) {
            boolean result = false;
            return new BooleanValue(context, result, context.getFormulaManager().getBooleanFormulaManager().makeBoolean(false));
        }
    }

    /**
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#compareTo(java.lang.Float)">compareTo</a>().
     * Compares this Float instance with another Float instance.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature
     * @return IntValue representing the comparison result (-1, 0, or 1)
     */
    private Value<?, ?> invokeCompareTo(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        if (args.length == 1) {
            FloatObjectValue other = args[0].asFloatObjectValue();
            int result = val.concrete.compareTo(other.val.concrete);
            
            // Use floating point formula managers for proper symbolic handling
            BooleanFormula equal = fpfm.equalWithFPSemantics(val.formula, other.val.formula);
            BooleanFormula greater = fpfm.greaterThan(val.formula, other.val.formula);
            
            // Create the symbolic result using if-then-else logic
            var imgr = context.getFormulaManager().getIntegerFormulaManager();
            var symbolicResult = context.getFormulaManager().getBooleanFormulaManager().ifThenElse(equal,
                imgr.makeNumber(0),
                context.getFormulaManager().getBooleanFormulaManager().ifThenElse(greater, 
                    imgr.makeNumber(1), 
                    imgr.makeNumber(-1)));
                
            return new IntValue(context, result, symbolicResult);
        } else {
            return PlaceHolder.instance;
        }
    }

    /**
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#isNaN()">isNaN</a>().
     * Returns true if this Float value is a Not-a-Number (NaN).
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return BooleanValue representing the NaN check result
     */
    private Value<?, ?> invokeIsNaN(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for isNaN() method");
        boolean result = val.concrete.isNaN();
        return new BooleanValue(context, result, fpfm.isNaN(val.formula));
    }

    /**
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#isInfinite()">isInfinite</a>().
     * Returns true if this Float value is infinitely large in magnitude.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return BooleanValue representing the infinity check result
     */
    private Value<?, ?> invokeIsInfinite(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for isInfinite() method");
        boolean result = val.concrete.isInfinite();
        return new BooleanValue(context, result, fpfm.isInfinity(val.formula));
    }

    /**
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#describeConstable()">describeConstable</a>().
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
     * Invocation handling for the Float instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html#resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup)">resolveConstantDesc</a>().
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
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof FloatObjectValue other) {
            return (val.equals(other.val));
        } else if (o instanceof FloatValue other) {
            return val.equals(other);
        } else {
            return false;
        }
    }

    @Override
    public FloatValue asFloatValue() {
        return val;
    }

    @Override
    public FloatObjectValue asFloatObjectValue() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(val);
    }


    @Override
    public String toString() {
        return "Ljava/lang/Float; @" + Integer.toHexString(address) + " -> " + val;
    }
}
