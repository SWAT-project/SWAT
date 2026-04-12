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
import de.uzl.its.swat.symbolic.value.reference.lang.FloatObjectValue;
import lombok.Getter;

public class FloatValue extends NumericalValue<FloatingPointFormula, Float> {
    @Getter
    private static final String symbolicPrefix = "F";

    public static final FormulaType.FloatingPointType precision =
            FormulaType.getSinglePrecisionFloatingPointType();
    private FloatingPointFormulaManager fmgr;

    public FloatValue(SolverContext context, float concrete) {
        this.context = context;
        this.fmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        // ToDo (Nils): What rounding strategy does java use. It should be reflected here
        this.formula = fmgr.makeNumber(concrete, precision);
    }

    public FloatValue(SolverContext context, float concrete, FloatingPointFormula formula) {
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
     * Turns this IntValue into a symbolic variable
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
     * Adds two floats
     *
     * @param f The other float
     * @return The resulting FloatValue
     */
    public FloatValue FADD(FloatValue f) {
        // ToDo (Nils): Should we specify rounding type here?
        return new FloatValue(context, concrete + f.concrete, fmgr.add(formula, f.formula));
    }

    /**
     * Divides two floats
     *
     * @param f The other float
     * @return The resulting FloatValue
     */
    public FloatValue FDIV(FloatValue f) {
        return new FloatValue(context, concrete / f.concrete, fmgr.divide(formula, f.formula));
    }

    /**
     * Multiplies two floats
     *
     * @param f The other float
     * @return The resulting FloatValue
     */
    public FloatValue FMUL(FloatValue f) {
        return new FloatValue(context, concrete * f.concrete, fmgr.multiply(formula, f.formula));
    }

    /**
     * Negates a float
     *
     * @return The resulting FloatValue
     */
    public FloatValue FNEG() {
        return new FloatValue(context, -concrete, fmgr.negate(formula));
    }

    /**
     * Gets the remainder from the division of two floats WARNING: This is not symbolically tracked!
     *
     * @param f The other flaot
     * @return The resulting FloatValue
     */
    public FloatValue FREM(FloatValue f) {
        return new FloatValue(context, concrete % f.concrete);
    }

    /**
     * Subtracts two floats
     *
     * @param f The other float
     * @return The resulting FloatValue
     */
    public FloatValue FSUB(FloatValue f) {
        return new FloatValue(context, concrete - f.concrete, fmgr.subtract(formula, f.formula));
    }

    /**
     * Casts a float to an integer with JVM saturation semantics.
     * Per JVM Spec 2.8: NaN → 0, +Inf/too large → Int.MAX_VALUE, -Inf/too small → Int.MIN_VALUE
     *
     * @return The resulting IntValue
     */
    public IntValue F2I() {
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
        // Note: (float)Integer.MAX_VALUE = 2^31 (rounded up from 2^31-1), which is OUT of range
        // Any float >= 2^31 must saturate to Integer.MAX_VALUE
        // Integer.MIN_VALUE (-2^31) CAN be exactly represented in float, so use > for lower bound
        FloatingPointFormula fpMax = fpmgr.makeNumber((float) Integer.MAX_VALUE, precision);  // = 2^31
        FloatingPointFormula fpMin = fpmgr.makeNumber((float) Integer.MIN_VALUE, precision);  // = -2^31 (exact)
        BooleanFormula isFinite = bmgr.and(bmgr.not(fpmgr.isNaN(formula)), bmgr.not(fpmgr.isInfinity(formula)));
        // Use >= for upper bound because fpMax (2^31) is already out of range
        BooleanFormula tooLarge = bmgr.and(isFinite, fpmgr.greaterOrEquals(formula, fpMax));
        // Use < for lower bound because fpMin (-2^31) is exactly representable and in range
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
     * Casts a float to a double
     *
     * @return The resulting IntValue
     */
    public DoubleValue F2D() {
        // ToDo (Nils): Does that work as expected? What about rounding strategy?
        return new DoubleValue(
                context,
                concrete.doubleValue(),
                fmgr.castFrom(formula, true, DoubleValue.precision));
    }

    /**
     * Casts a float to a long with JVM saturation semantics.
     * Per JVM Spec 2.8: NaN → 0, +Inf/too large → Long.MAX_VALUE, -Inf/too small → Long.MIN_VALUE
     *
     * @return The resulting LongValue
     */
    public LongValue F2L() {
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
        // Note: (float)Long.MAX_VALUE = 2^63 (rounded up from 2^63-1), which is OUT of range
        // Any float >= 2^63 must saturate to Long.MAX_VALUE
        // Long.MIN_VALUE (-2^63) CAN be exactly represented in float, so use > for lower bound
        FloatingPointFormula fpMax = fpmgr.makeNumber((float) Long.MAX_VALUE, precision);  // = 2^63
        FloatingPointFormula fpMin = fpmgr.makeNumber((float) Long.MIN_VALUE, precision);  // = -2^63 (exact)
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

    /**
     * Compares two floats and returns: 0 if both values are equal; 1 if this float is greater or
     * either float is NaN; -1 if the parameter is greater ToDo (Nils): This method is not validated
     *
     * @param f The other FloatValue
     * @return The resulting IntValue with the boolean conditions
     */
    public IntValue FCMPG(FloatValue f) {
        if (concrete.isNaN() || f.concrete.isNaN()) {
            return new IntValue(context, 1);
        }
        return FCMP(f);
    }

    /**
     * Compares two floats and returns: 0 if both values are equal 1 if this float is greater -1 if
     * the parameter is greater or either float is NaN ToDo (Nils): This method is not validated
     *
     * @param f The other FloatValue
     * @return The resulting IntValue with the boolean conditions
     */
    public IntValue FCMPL(FloatValue f) {
        if (concrete.isNaN() || f.concrete.isNaN()) {
            return new IntValue(context, -1);
        }
        return FCMP(f);
    }

    /**
     * Compares two floats and returns: 0 if both values are equal 1 if this float is greater -1 if
     * the parameter is greater
     *
     * @param f The other FloatValue
     * @return The resulting IntValue with the boolean conditions
     */
    private IntValue FCMP(FloatValue f) {
        int c = concrete.compareTo(f.concrete);

        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        BooleanFormula eqCond = fmgr.equalWithFPSemantics(formula, f.formula);
        BooleanFormula gtCond = fmgr.greaterThan(formula, f.formula);
        NumeralFormula.IntegerFormula tmp =
                bmgr.ifThenElse(gtCond, imgr.makeNumber(1), imgr.makeNumber(-1));
        NumeralFormula.IntegerFormula res = bmgr.ifThenElse(eqCond, imgr.makeNumber(0), tmp);

        return new IntValue(context, c, res);
    }


    /**
     * Converts this FloatValue to a ByteValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: double → int (RTZ) 
     * Per JVM Spec 2.11.4 int → byte (discard all but lowest 8 bits)
     *
     * @return The resulting ByteValue
     */
    public ByteValue asByteValue() {
        // Step 1: float → int using fpToIntFormula (eliminates UF)
        NumeralFormula.IntegerFormula intFormula = fpToIntFormula(formula, 32);

        // Step 2: int → byte 
        byte byteVal = (byte) concrete.intValue();

        // Java byte is signed: -128 to 127
        NumeralFormula.IntegerFormula mod256 = imgr.modulo(intFormula, imgr.makeNumber(256));

        // Adjust to signed byte range: if > 127, subtract 256
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula isGreaterThan127 = imgr.greaterThan(mod256, imgr.makeNumber(127));
        NumeralFormula.IntegerFormula byteFormula = bmgr.ifThenElse(
            isGreaterThan127,
            imgr.subtract(mod256, imgr.makeNumber(256)),
            mod256
        );

        return new ByteValue(context, byteVal, byteFormula);
    }

    /**
     * Converts this FloatValue to a ShortValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: double → int (RTZ) 
     * Per JVM Spec 2.11.4 int → short (discard all but lowest 16 bits)
     *
     * @return The resulting ShortValue
     */
    @Override
    public ShortValue asShortValue() {

        // float → int 
        NumeralFormula.IntegerFormula intFormula = fpToIntFormula(formula, 32);

        // int → short 
        short shortVal = (short) concrete.intValue();

        // Java short is signed: -32768 to 32767
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
     * Converts this FloatValue to an IntValue.
     * Per JVM Spec 2.8: float → int (RTZ)
     *
     * @return The resulting IntValue
     */
    @Override
    public IntValue asIntValue() {
        return F2I();
    }

    /**
     * Converts this FloatValue to a LongValue.
     * Per JVM Spec 2.8: float → long (RTZ with saturation for special cases)
     *
     * @return The resulting LongValue
     */
    @Override
    public LongValue asLongValue() {
        return F2L();
    }

    /**
     * Converts this FloatValue to a CharValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: double → int (RTZ) 
     * Per JVM Spec 2.11.4 int → char (discard all but lowest 16 bits)
     *
     * @return The resulting CharValue
     */
    @Override
    public CharValue asCharValue() {

        // Step 1: float → int using fpToIntFormula (eliminates UF)
        NumeralFormula.IntegerFormula intFormula = fpToIntFormula(formula, 32);

        // Step 2: int → char 
        char charVal = (char) concrete.intValue();

        // modulo 65536 for unsigned 16-bit
        NumeralFormula.IntegerFormula charFormula = imgr.modulo(intFormula, imgr.makeNumber(65536));

        return new CharValue(context, charVal, charFormula);
    }

    @Override
    public FloatValue asFloatValue() {
        return this;
    }

    @Override
    public DoubleValue asDoubleValue() {
        return F2D();
    }

    //@Override
    //public StringValue asStringValue() {
    //    return new StringValue(context, String.valueOf(concrete), -1);
    //}

    @Override
    public FloatObjectValue asObjectValue(){
        return new FloatObjectValue(context, this, ObjectValue.ADDRESS_UNKNOWN);
    }

    @Override
    public String getConcreteEncoded() {
        int intBits = Float.floatToIntBits(concrete);
        return Integer.toHexString(intBits);
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
        return genericToString("F");
    }
}
