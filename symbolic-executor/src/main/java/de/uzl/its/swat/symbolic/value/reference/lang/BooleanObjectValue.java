package de.uzl.its.swat.symbolic.value.reference.lang;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.thread.ThreadHandler;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

public class BooleanObjectValue extends BoxedValue<BooleanValue> {

    private final BooleanFormulaManager bfm;

    public BooleanObjectValue(SolverContext ctx, BooleanValue val, int address) {
        super(ctx, address);
        SWATAssert.enforce(val != null, "Boolean has to be instantiated with default value!");
        this.val = val;
        this.bfm = ctx.getFormulaManager().getBooleanFormulaManager();
    }

    /**
     * Gets the bound of the primitive type
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        assert val != null : "[SWAT] Cannot create bound for uninitialized value!";
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
        assert val != null : "[SWAT] Cannot make uninitialized value symbolic!";
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
        assert val != null : "[SWAT] Cannot make uninitialized value symbolic!";
        return val.MAKE_SYMBOLIC();
    }

    /**
     * Handles method invocation for Java's <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html">Boolean</a>
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
            case "booleanValue" -> invokeBooleanValue(args, desc);
            case "toString" -> invokeToString(args, desc);
            case "hashCode" -> invokeHashCode(args, desc);
            case "equals" -> invokeEquals(args, desc);
            case "compareTo" -> invokeCompareTo(args, desc);
            default -> PlaceHolder.instance;
        };
    }

    private Value<?, ?> invokeInit(Value<?, ?>[] args, Type[] desc) {
        if (args[0] instanceof BooleanValue bv) {
            this.val = new BooleanValue(context, bv.concrete, bv.formula);
            return VoidValue.instance;
        }
        return PlaceHolder.instance;
    }

    /**
     * Invocation handling for the Boolean instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#booleanValue()">booleanValue</a>().
     * Returns the primitive boolean value of this Boolean object.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return The primitive BooleanValue
     */
    private Value<?, ?> invokeBooleanValue(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for booleanValue() method");
        return val;
    }

    /**
     * Invocation handling for the Boolean instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#toString()">toString</a>().
     * Returns a String representation of this Boolean object.
     *
     * @param args List of Values that correspond to the method arguments (should be empty)
     * @param desc Array of Type descriptions of the methods' signature
     * @return StringValue representing the string representation
     */
    private Value<?, ?> invokeToString(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 0, "Wrong number of arguments for toString() method");
        BooleanFormulaManager bfm = context.getFormulaManager().getBooleanFormulaManager();
        StringFormula sf = bfm.ifThenElse(
            val.formula,
            context.getFormulaManager().getStringFormulaManager().makeString("true"),
            context.getFormulaManager().getStringFormulaManager().makeString("false")
        );
        return new StringValue(context, val.concrete.toString(), sf, ADDRESS_UNKNOWN);
    }

    /**
     * Invocation handling for the Boolean instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#hashCode()">hashCode</a>().
     * Returns a hash code for this Boolean object.
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
     * Invocation handling for the Boolean instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#equals(java.lang.Object)">equals</a>().
     * Compares this Boolean object with another object for equality.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature
     * @return BooleanValue representing the equality result
     */
    private Value<?, ?> invokeEquals(Value<?, ?>[] args, Type[] desc) {
        SWATAssert.enforce(args.length == 1, "Wrong number of arguments for equals() method");

        Value<?, ?> other = args[0];
        try {
            BooleanValue otherBoolean = other.asBooleanValue();
            boolean result = val.concrete.equals(otherBoolean.concrete);
            return new BooleanValue(context, result, bfm.equivalence(val.formula, otherBoolean.formula));
        } catch (Exception ignored) {
            return PlaceHolder.instance; // If the other value is not a BooleanValue, return PlaceHolder
        }

    }

    /**
     * Invocation handling for the Boolean instance method <a
     * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html#compareTo(java.lang.Boolean)">compareTo</a>().
     * Compares this Boolean instance with another Boolean instance.
     *
     * @param args List of Values that correspond to the method arguments
     * @param desc Array of Type descriptions of the methods' signature
     * @return IntValue representing the comparison result (-1, 0, or 1)
     */
    private Value<?, ?> invokeCompareTo(Value<?, ?>[] args, Type[] desc) throws NotImplementedException {
        if (args.length == 1) {
            BooleanObjectValue other = args[0].asBooleanObjectValue();
            int result = val.concrete.compareTo(other.val.concrete);
            
            IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
            BooleanFormulaManager bfm = context.getFormulaManager().getBooleanFormulaManager();
            BooleanFormula thisTrue = val.formula;
            BooleanFormula otherTrue = other.val.formula;
            
            BooleanFormula bothEqual = bfm.equivalence(thisTrue, otherTrue);
            BooleanFormula thisGreater = bfm.and(thisTrue, bfm.not(otherTrue));
            
            NumeralFormula.IntegerFormula symbolicResult = bfm.ifThenElse(bothEqual,
                imgr.makeNumber(0),
                    bfm.ifThenElse(thisGreater, imgr.makeNumber(1), imgr.makeNumber(-1)));
                
            return new IntValue(context, result, symbolicResult);
        } else {
            return PlaceHolder.instance;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof BooleanObjectValue other) {
            return (val.equals(other.val));
        } else if (o instanceof BooleanValue other) {
            return val.equals(other);
        } else {
            return false;
        }
    }

    @Override
    public BooleanValue asBooleanValue() {
        return val;
    }

    @Override
    public BooleanObjectValue asBooleanObjectValue() {
        return this;
    }


    @Override
    public BooleanObjectValue asObjectValue() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(val);
    }

    @Override
    public String toString() {
        return "Ljava/lang/Boolean; @" + Integer.toHexString(address) + " -> " + val;
    }
}
