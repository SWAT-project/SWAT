package de.uzl.its.swat.symbolic.value.reference.lang;

import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.StringFormulaManager;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.VoidValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;

public class LongObjectValue extends BoxedValue<LongValue> {

    private BitvectorFormulaManager bvmgr;
    private StringFormulaManager sfm;

    public LongObjectValue(SolverContext ctx, LongValue val, int address) {
        super(ctx, address);
        SWATAssert.enforce(val != null, "Long has to be instantiated with default value!");
        this.bvmgr = ctx.getFormulaManager().getBitvectorFormulaManager();
        this.sfm = ctx.getFormulaManager().getStringFormulaManager();
        this.val = val;
    }

    /**
     * Gets the bound of the primitive type.
     * For BitvectorFormula, bounds are implicit (64-bit), so returns TRUE.
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

    /**
     * Compares if an arbitrary object is equal to this instance
     *
     * @param o The other object
     * @return Returns true if the objects are identical or if the other object is a long and has
     *     the same value as this one
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof LongObjectValue other) {
            return (val.equals(other.val));
        } else if (o instanceof LongValue other) {
            return val.equals(other);
        } else {
            return false;
        }
    }

    /**
     * Responsible for invoking the methods present in the java/lang/Long class
     *
     * @param name The Name of the method that should be invoked
     * @param args The methods arguments (if any)
     * @return The resulting Value
     */
    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {

        if (name.equals("<init>")) {
            return invokeInit(args, desc);
        } else if (name.equals("intValue")) {
            return val.asIntValue();
        } else if (name.equals("longValue")) {
            return new LongValue(context, val.concrete, val.formula);
        } else if (name.equals("shortValue")) {
            return val.asShortValue();
        } else if (name.equals("byteValue")) {
            return val.asByteValue();
        } else if (name.equals("equals")) {
            return invokeEquals(args);
        } else if (name.equals("doubleValue")) {
            return invokeDoubleValue();
        } else if (name.equals("floatValue")) {
            return invokeFloatValue();
        } else if (name.equals("toString")) {
            return invokeToString();
        } else if (name.equals("compareTo")) {
            return invokeCompareTo(args);
        }
        return PlaceHolder.instance;
    }

    private IntValue invokeCompareTo(Value<?, ?>[] args) {
        LongValue other = ((LongObjectValue) args[0]).val;
        int result = Long.compare(this.val.concrete, other.concrete);

        // Long.compareTo returns -1, 0, or 1 (not actual difference, to avoid overflow)
        BooleanFormulaManager bfm = context.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager ifm = context.getFormulaManager().getIntegerFormulaManager();

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
        // Delegate to LongValue.L2F() which uses direct BV→FP conversion
        return this.val.L2F();
    }

    private DoubleValue invokeDoubleValue() {
        // Delegate to LongValue.L2D() which uses direct BV→FP conversion
        return this.val.L2D();
    }

    private Value<?, ?> invokeInit(Value<?, ?>[] args, Type[] desc) {
        if (args[0] instanceof LongValue lv) {
            this.val = new LongValue(context, lv.concrete, lv.formula);
            return VoidValue.instance;
        }
        return PlaceHolder.instance;
    }

    private BooleanValue invokeEquals(Value<?, ?>[] args) {
        if (args[0] instanceof LongObjectValue) {
            return new BooleanValue(
                    this.context,
                    ((LongObjectValue) args[0]).val.concrete.equals(this.val.concrete),
                    this.bvmgr.equal(((LongObjectValue) args[0]).val.formula, this.val.formula));
        } else {
            return new BooleanValue(this.context, false);
        }
    }

    @Override
    public LongObjectValue asLongObjectValue() {
        return this;
    }

    @Override
    public String toString() {
        return "Ljava/lang/Long; @" + Integer.toHexString(address);
    }
}
