package de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint;

import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;

import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ByteValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.DoubleObjectValue;
import lombok.Getter;

/**
 * Wrapper for doubles to represent symbolic and concrete information on the symbolic stack
 *
 * @author Nils Loose
 * @version 2022.07.09
 */
public class DoubleValue extends NumericalValue<FloatingPointFormula, Double> {
    @Getter
    private static final String symbolicPrefix = "D";
    public static final FormulaType.FloatingPointType precision =
            FormulaType.getDoublePrecisionFloatingPointType();
    private FloatingPointFormulaManager fmgr;

    /**
     * Constructor that creates a new DoubleValue without prior symbolic information and only a
     * concrete value
     *
     * @param context The Java-smt solver context
     * @param concrete The concrete double value
     */
    public DoubleValue(SolverContext context, double concrete) {
        this.context = context;
        this.fmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        // ToDo (Nils): What rounding strategy does java use. It should be reflected here
        this.formula = fmgr.makeNumber(concrete, precision);
    }

    /**
     * Constructor that creates a new DoubleValue with prior symbolic information and a concrete
     * value
     *
     * @param context The Java-smt solver context
     * @param concrete The concrete double value
     * @param formula Prior symbolic information
     */
    public DoubleValue(SolverContext context, double concrete, FloatingPointFormula formula) {
        this.context = context;
        this.fmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Turns this DoubleValue into a symbolic variable
     *
     * @param prefixOrIdx
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(String prefixOrIdx) {
        if (prefixOrIdx.matches("-?\\d+")){
            // We assume a constructed idx was passed as it is a number
            initSymbolic(symbolicPrefix, prefixOrIdx);
        } else if (prefixOrIdx.matches(".*-?\\d+")){
            // Its a list which already has prefix and idx
            initSymbolicWithoutIdx(prefixOrIdx);
        } else {
            // If it's not a number we assume prefix
            initSymbolic(prefixOrIdx);
        }
        formula = fmgr.makeVariable(name, precision);
        return name;
    }

    /**
     * Turns this DoubleValue into a symbolic variable
     *
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC() {
        initSymbolic(symbolicPrefix);
        formula = fmgr.makeVariable(name, precision);
        return name;
    }
    /**
     * Turns this DoubleValue into a symbolic variable
     *
     * @param idx
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(long idx) {
        initSymbolic(symbolicPrefix, idx);
        formula = fmgr.makeVariable(name, precision);
        return name;
    }

    /**
     * Creates a formula that asserts that this symbolic value is within the bounds of this type.
     * For FloatingPointFormula, bounds are implicit, so we return TRUE.
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check (always TRUE)
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        return context.getFormulaManager().getBooleanFormulaManager().makeBoolean(true);
    }
    /**
     * Adds two doubles
     *
     * @param d The other double
     * @return The resulting DoubleValue
     */
    public DoubleValue DADD(DoubleValue d) {
        // ToDo (Nils): Should we specify rounding type here?
        return new DoubleValue(context, concrete + d.concrete, fmgr.add(formula, d.formula));
    }

    /**
     * Subtracts two doubles
     *
     * @param d The other double
     * @return The resulting DoubleValue
     */
    public DoubleValue DSUB(DoubleValue d) {
        return new DoubleValue(context, concrete - d.concrete, fmgr.subtract(formula, d.formula));
    }

    /**
     * Multiplies two doubles
     *
     * @param d The other double
     * @return The resulting DoubleValue
     */
    public DoubleValue DMUL(DoubleValue d) {
        return new DoubleValue(context, concrete * d.concrete, fmgr.multiply(formula, d.formula));
    }

    /**
     * Divides two doubles
     *
     * @param d The other double
     * @return The resulting DoubleValue
     */
    public DoubleValue DDIV(DoubleValue d) {
        return new DoubleValue(context, concrete / d.concrete, fmgr.divide(formula, d.formula));
    }

    /**
     * Gets the remainder from the division of two doubles WARNING: This is not symbolically
     * tracked!
     *
     * @param d The other double
     * @return The resulting DoubleValue
     */
    public DoubleValue DREM(DoubleValue d) {
        // ToDo (Nils): This is not (yet) symbolically tracked!
        // See https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/64
        return new DoubleValue(context, concrete % d.concrete);
    }

    /**
     * Negates a double
     *
     * @return The resulting DoubleValue
     */
    public DoubleValue DNEG() {
        return new DoubleValue(context, -concrete, fmgr.negate(formula));
    }

    /**
     * DCMPL instruction implementation.
     * Compares two doubles and pushes the result onto the operand stack.
     * If either value is NaN, pushes -1.
     *
     * @param f The other DoubleValue
     * @return The resulting IntValue with the comparison result
     */
    public IntValue DCMPL(DoubleValue f) {
        return DCMP(f, -1); // NaN result is -1 for DCMPL
    }

    /**
     * DCMPG instruction implementation.
     * Compares two doubles and pushes the result onto the operand stack.
     * If either value is NaN, pushes 1.
     *
     * @param f The other DoubleValue
     * @return The resulting IntValue with the comparison result
     */
    public IntValue DCMPG(DoubleValue f) {
        return DCMP(f, 1); // NaN result is 1 for DCMPG
    }

    /**
     * Compares two doubles and returns:
     * - nanResult if either value is NaN
     * - 0 if both values are equal
     * - 1 if this double is greater
     * - -1 if the parameter is greater
     *
     * @param f The other DoubleValue
     * @param nanResult The result to push if NaN is involved
     * @return The resulting IntValue with the boolean conditions
     */
    private IntValue DCMP(DoubleValue f, int nanResult) {
        int c;
        if (Double.isNaN(concrete) || Double.isNaN(f.concrete)) {
            c = nanResult;
        } else if (concrete > f.concrete) {
            c = 1;
        } else if (concrete == f.concrete) {
            c = 0;
        } else {
            c = -1;
        }

        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        FloatingPointFormulaManager fmgr = context.getFormulaManager().getFloatingPointFormulaManager();

        BooleanFormula isNaN1 = fmgr.isNaN(formula);
        BooleanFormula isNaN2 = fmgr.isNaN(f.formula);
        BooleanFormula isNaN = bmgr.or(isNaN1, isNaN2);

        BooleanFormula gtCond = fmgr.greaterThan(formula, f.formula);
        BooleanFormula eqCond = fmgr.equalWithFPSemantics(formula, f.formula);

        NumeralFormula.IntegerFormula res = bmgr.ifThenElse(
                isNaN,
                imgr.makeNumber(nanResult),
                bmgr.ifThenElse(
                        gtCond,
                        imgr.makeNumber(1),
                        bmgr.ifThenElse(
                                eqCond,
                                imgr.makeNumber(0),
                                imgr.makeNumber(-1)
                        )
                )
        );

        return new IntValue(context, c, res);
    }

    /**
     * Casts a double to an integer with JVM saturation semantics.
     * Per JVM Spec 2.8: NaN → 0, +Inf/too large → Int.MAX_VALUE, -Inf/too small → Int.MIN_VALUE
     *
     * @return The resulting IntValue
     */
    public IntValue D2I() {
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();

        // Cast FP to 32-bit bitvector using round-toward-zero (normal case)
        BitvectorFormula bvNormal = fpmgr.castTo(
                formula,
                true,  // signed
                FormulaType.getBitvectorTypeWithSize(32),
                FloatingPointRoundingMode.TOWARD_ZERO);

        // Handle JVM saturation semantics for special cases
        BooleanFormula isNaN = fpmgr.isNaN(formula);
        BooleanFormula isPosInf = bmgr.and(fpmgr.isInfinity(formula), bmgr.not(fpmgr.isNegative(formula)));
        BooleanFormula isNegInf = bmgr.and(fpmgr.isInfinity(formula), fpmgr.isNegative(formula));

        // Check for out-of-range values (finite but too large/small for int)
        // Integer.MAX_VALUE and Integer.MIN_VALUE CAN be exactly represented in double
        // Use > for upper bound (values > MAX are out of range)
        // Use < for lower bound (values < MIN are out of range)
        FloatingPointFormula fpMax = fpmgr.makeNumber((double) Integer.MAX_VALUE, precision);
        FloatingPointFormula fpMin = fpmgr.makeNumber((double) Integer.MIN_VALUE, precision);
        BooleanFormula isFinite = bmgr.and(bmgr.not(fpmgr.isNaN(formula)), bmgr.not(fpmgr.isInfinity(formula)));
        BooleanFormula tooLarge = bmgr.and(isFinite, fpmgr.greaterThan(formula, fpMax));
        BooleanFormula tooSmall = bmgr.and(isFinite, fpmgr.lessThan(formula, fpMin));

        // Bitvector constants for saturation
        BitvectorFormula bvZero = bvmgr.makeBitvector(32, 0);
        BitvectorFormula bvMax = bvmgr.makeBitvector(32, Integer.MAX_VALUE);
        BitvectorFormula bvMin = bvmgr.makeBitvector(32, Integer.MIN_VALUE);

        // Apply saturation: NaN→0, +Inf/tooLarge→MAX, -Inf/tooSmall→MIN, else normal
        BitvectorFormula bv32 = bmgr.ifThenElse(isNaN, bvZero,
                bmgr.ifThenElse(bmgr.or(isPosInf, tooLarge), bvMax,
                        bmgr.ifThenElse(bmgr.or(isNegInf, tooSmall), bvMin, bvNormal)));

        return new IntValue(context, concrete.intValue(), bv32);
    }

    /**
     * Casts a double to a float
     *
     * @return The resulting FloatValue
     */
    public FloatValue D2F() {
        // ToDo (Nils): Choose a rounding strategy?
        return new FloatValue(
                context, concrete.floatValue(), fmgr.castFrom(formula, true, FloatValue.precision));
    }

    /**
     * Casts a double to a long with JVM saturation semantics.
     * Per JVM Spec 2.8: NaN → 0, +Inf/too large → Long.MAX_VALUE, -Inf/too small → Long.MIN_VALUE
     *
     * @return The resulting LongValue
     */
    public LongValue D2L() {
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();

        // Cast FP to 64-bit bitvector using round-toward-zero (normal case)
        BitvectorFormula bvNormal = fpmgr.castTo(
                formula,
                true,  // signed
                FormulaType.getBitvectorTypeWithSize(64),
                FloatingPointRoundingMode.TOWARD_ZERO);

        // Handle JVM saturation semantics for special cases
        BooleanFormula isNaN = fpmgr.isNaN(formula);
        BooleanFormula isPosInf = bmgr.and(fpmgr.isInfinity(formula), bmgr.not(fpmgr.isNegative(formula)));
        BooleanFormula isNegInf = bmgr.and(fpmgr.isInfinity(formula), fpmgr.isNegative(formula));

        // Check for out-of-range values (finite but too large/small for long)
        // Note: (double)Long.MAX_VALUE = 2^63 (rounded up from 2^63-1), which is OUT of range
        // Any double >= 2^63 must saturate to Long.MAX_VALUE
        // Long.MIN_VALUE (-2^63) CAN be exactly represented in double, so use > for lower bound
        FloatingPointFormula fpMax = fpmgr.makeNumber((double) Long.MAX_VALUE, precision);  // = 2^63
        FloatingPointFormula fpMin = fpmgr.makeNumber((double) Long.MIN_VALUE, precision);  // = -2^63 (exact)
        BooleanFormula isFinite = bmgr.and(bmgr.not(fpmgr.isNaN(formula)), bmgr.not(fpmgr.isInfinity(formula)));
        // Use >= for upper bound because fpMax (2^63) is already out of range
        BooleanFormula tooLarge = bmgr.and(isFinite, fpmgr.greaterOrEquals(formula, fpMax));
        // Use < for lower bound because fpMin (-2^63) is exactly representable and in range
        BooleanFormula tooSmall = bmgr.and(isFinite, fpmgr.lessThan(formula, fpMin));

        // Bitvector constants for saturation
        BitvectorFormula bvZero = bvmgr.makeBitvector(64, 0L);
        BitvectorFormula bvMax = bvmgr.makeBitvector(64, Long.MAX_VALUE);
        BitvectorFormula bvMin = bvmgr.makeBitvector(64, Long.MIN_VALUE);

        // Apply saturation: NaN→0, +Inf/tooLarge→MAX, -Inf/tooSmall→MIN, else normal
        BitvectorFormula bv64 = bmgr.ifThenElse(isNaN, bvZero,
                bmgr.ifThenElse(bmgr.or(isPosInf, tooLarge), bvMax,
                        bmgr.ifThenElse(bmgr.or(isNegInf, tooSmall), bvMin, bvNormal)));

        return new LongValue(context, concrete.longValue(), bv64);
    }

    @Override
    public String getConcreteEncoded() {
        long longBits = Double.doubleToLongBits(concrete);
        return Long.toHexString(longBits);
    }

    
    /**
     * Converts this DoubleValue to a ByteValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: double → int (RTZ) 
     * Per JVM Spec 2.11.4 int → byte (discard all but lowest 8 bits)
     *
     * @return The resulting ByteValue
     */
    @Override
    public ByteValue asByteValue() {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();

        // double → int using fpToIntFormula (avoids UF in castTo)
        NumeralFormula.IntegerFormula intFormula = fpToIntFormula(formula, 32);

        // int → byte
        NumeralFormula.IntegerFormula mod256 = imgr.modulo(intFormula, imgr.makeNumber(256));

        // Adjust for signed byte: if result >= 128, subtract 256 to get negative value
        BooleanFormula isNegative = imgr.greaterOrEquals(mod256, imgr.makeNumber(128));
        NumeralFormula.IntegerFormula byteFormula = bmgr.ifThenElse(
                isNegative,
                imgr.subtract(mod256, imgr.makeNumber(256)),
                mod256
        );

        return new ByteValue(context, concrete.byteValue(), byteFormula);
    }

    /**
     * Converts this DoubleValue to a ShortValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: double → int (RTZ) 
     * Per JVM Spec 2.11.4 int → short (discard high 16 bits)
     *
     * @return The resulting ShortValue
     */
    @Override
    public ShortValue asShortValue() {

        // double → int using fpToIntFormula (avoids UF in castTo)
        NumeralFormula.IntegerFormula intFormula = fpToIntFormula(formula, 32);

        // int → short
        short shortVal = (short) concrete.intValue();

        NumeralFormula.IntegerFormula mod65536 = imgr.modulo(intFormula, imgr.makeNumber(65536));

        // Adjust to signed short range: if > 32767, subtract 65536
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula isGreaterThan32767 = imgr.greaterThan(mod65536, imgr.makeNumber(32767));
        NumeralFormula.IntegerFormula shortFormula = bmgr.ifThenElse(
            isGreaterThan32767,
            imgr.subtract(mod65536, imgr.makeNumber(65536)),
            mod65536
        );

        return new ShortValue(context, shortVal, shortFormula);
    }

     /**
     * Converts this DoubleValue to a CharValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: double → int (RTZ) 
     * Per JVM Spec 2.11.4 int → char (discard high 16 bits)
     *
     * @return The resulting CharValue
     */
    @Override
    public CharValue asCharValue() {

        // double → int using fpToIntFormula (avoids UF in castTo)
        NumeralFormula.IntegerFormula intFormula = fpToIntFormula(formula, 32);

        // int → char 
        char charVal = (char) concrete.intValue();

        // modulo 65536 for unsigned 16-bit
        NumeralFormula.IntegerFormula charFormula = imgr.modulo(intFormula, imgr.makeNumber(65536));

        return new CharValue(context, charVal, charFormula);
    }

    @Override
    public DoubleValue asDoubleValue() {
        return this;
    }

    @Override
    public FloatValue asFloatValue() {
        return D2F();
    }

    /**
     * Converts this DoubleValue to an IntValue.
     *
     * @return The resulting IntValue
     */
    @Override
    public IntValue asIntValue() {
        return D2I();
    }

    /**
     * Converts this DoubleValue to a LongValue.
     *
     * @return The resulting LongValue
     */
    @Override
    public LongValue asLongValue() {
        return D2L();
    }

    //@Override
    //public StringValue asStringValue() {
    //    return new StringValue(context, String.valueOf(concrete), -1);
    //}

    @Override
    public DoubleObjectValue asObjectValue(){
        return new DoubleObjectValue(context, this, ObjectValue.ADDRESS_UNKNOWN);
    }

    @Override
    public String getSymPrefix(){
        return symbolicPrefix;
    }

    /**
     * Returns the string representation of the value used to visualize the stack. The
     * representation is not complete.
     *
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("D");
    }
}
