package de.uzl.its.swat.symbolic.value.reference.lang;

import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.*;

public final class IntegerObjectValue extends BoxedValue<IntValue> {

    private BitvectorFormulaManager bvmgr;
    private IntegerFormulaManager ifm;
    private StringFormulaManager sfm;

    public IntegerObjectValue(SolverContext ctx, IntValue val, int address) {
        super(ctx, address);
        SWATAssert.enforce(val != null, "Integer has to be instantiated with default value!");
        this.val = val;
        this.bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        this.ifm = ctx.getFormulaManager().getIntegerFormulaManager();
        this.sfm = ctx.getFormulaManager().getStringFormulaManager();
    }

    /**
     * Gets the bound of the primitive type.
     * For BitvectorFormula, bounds are implicit (32-bit), so returns TRUE.
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check (always TRUE for bitvectors)
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
    public String MAKE_SYMBOLIC(String prefixOrIdx) {
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
    public String MAKE_SYMBOLIC() {
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
        if (o instanceof IntegerObjectValue other) {
            return (val.equals(other.val));
        } else if (o instanceof IntValue other) {
            return val.equals(other);
        } else {
            return false;
        }
    }

    /**
     * @param name
     * @param args
     * @return
     */
    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) {

        return switch (name) {
            case "<init>" -> invokeInit(args);
            case "intValue" ->
                // Return a copy of the IntValue
                    new IntValue(context, val.concrete, val.formula);
            case "longValue" -> val.asLongValue();
            case "shortValue" -> val.asShortValue();
            case "byteValue" -> val.asByteValue();
            case "equals" -> invokeEquals(args);
            case "doubleValue" -> invokeDoubleValue();
            case "floatValue" -> invokeFloatValue();
            case "toString" -> invokeToString();
            case "compareTo" -> invokeCompareTo(args);
            default -> PlaceHolder.instance;
        };

    }

    private IntValue invokeCompareTo(Value<?, ?>[] args) {
        IntValue other = ((IntegerObjectValue) args[0]).val;
        int result = Integer.compare(this.val.concrete, other.concrete);

        // Integer.compareTo returns -1, 0, or 1 (not actual difference, to avoid overflow)
        BooleanFormulaManager bfm = context.getFormulaManager().getBooleanFormulaManager();
        // Use signed bitvector comparison
        BooleanFormula equal = bvmgr.equal(this.val.formula, other.formula);
        BooleanFormula greater = bvmgr.greaterThan(this.val.formula, other.formula, true); // signed

        NumeralFormula.IntegerFormula symbolicResult = bfm.ifThenElse(equal,
                ifm.makeNumber(0),
                bfm.ifThenElse(greater, ifm.makeNumber(1), ifm.makeNumber(-1)));

        return new IntValue(this.context, result, symbolicResult);
    }

    private StringValue invokeToString() {
        // Convert BV to Int for string theory
        NumeralFormula.IntegerFormula intFormula = bvmgr.toIntegerFormula(this.val.formula, true);
        return new StringValue(
                this.context,
                this.val.concrete.toString(),
                integralToStringFormula(intFormula),
                -1);
    }

    private FloatValue invokeFloatValue() {
        // Delegate to IntValue.I2F() which uses direct BV→FP conversion
        return this.val.I2F();
    }

    private DoubleValue invokeDoubleValue() {
        // Delegate to IntValue.I2D() which uses direct BV→FP conversion
        return this.val.I2D();
    }

    private Value<?, ?> invokeInit(Value<?, ?>[] args) {
        if (args[0] instanceof IntValue iv) {
            this.val = new IntValue(this.context, iv.concrete, iv.formula);
            return VoidValue.instance;
        } else if (args[0] instanceof StringValue sv) {
            // Parse string to int, convert to bitvector
            this.val = new IntValue(
                    this.context,
                    Integer.parseInt(sv.concrete),
                    bvmgr.makeBitvector(32, this.sfm.toIntegerFormula(sv.formula)));
            return VoidValue.instance;
        }
        return PlaceHolder.instance;
    }

    private BooleanValue invokeEquals(Value<?, ?>[] args) {
        if (args[0] instanceof IntegerObjectValue) {
            return new BooleanValue(
                    this.context,
                    ((IntegerObjectValue) args[0]).val.concrete.equals(this.val.concrete),
                    this.bvmgr.equal(((IntegerObjectValue) args[0]).val.formula, this.val.formula));
        } else {
            return new BooleanValue(this.context, false);
        }
    }

    @Override
    public IntegerObjectValue asIntegerObjectValue() {
        return this;
    }

    @Override
    public IntValue asIntValue() {
        return val;
    }


    @Override
    public String toString() {
        return "Ljava/lang/Integer; @" + Integer.toHexString(address) + " -> " + val;
    }
}
