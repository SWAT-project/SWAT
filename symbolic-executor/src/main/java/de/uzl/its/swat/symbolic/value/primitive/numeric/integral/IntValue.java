package de.uzl.its.swat.symbolic.value.primitive.numeric.integral;

import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.IntegerObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import lombok.Getter;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper to represent int values (32-bit signed) on the symbolic stack.
 */
public class IntValue extends NumericalValue<BitvectorFormula, Integer> {
    @Getter
    private static final String symbolicPrefix = "I";

    /** Bit width for int values */
    private static final int BIT_WIDTH = 32;

    /** Bitvector formula manager for handling 32-bit bitvector formulas */
    protected BitvectorFormulaManager bvmgr;

    /**
     * Instantiates a new Int value.
     *
     * @param context the context
     * @param concrete the concrete
     */
    public IntValue(SolverContext context, int concrete) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, concrete);
    }

    /**
     * Instantiates a new Int value with a bitvector formula.
     *
     * @param context the context
     * @param concrete the concrete
     * @param formula the bitvector formula
     */
    public IntValue(SolverContext context, int concrete, BitvectorFormula formula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Instantiates a new Int value from an IntegerFormula (for backward compatibility).
     * Converts the IntegerFormula to BitvectorFormula.
     *
     * @param context the context
     * @param concrete the concrete
     * @param intFormula the integer formula to convert
     */
    public IntValue(SolverContext context, int concrete, NumeralFormula.IntegerFormula intFormula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        // Convert IntegerFormula to BitvectorFormula
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, intFormula);
    }

    /**
     * Turns this IntValue into a symbolic variable
     *
     * @param prefixOrIdx
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(String prefixOrIdx) {
        if (prefixOrIdx.matches("-?\\d+")){
            // We assume a constructed idx was passed as it is a number
            initSymbolic(symbolicPrefix, prefixOrIdx);
        } else if (prefixOrIdx.matches(".*-?\\d+") || prefixOrIdx.contains("_length")){
            // It is a list or the length of an array which already has prefix and idx
            initSymbolicWithoutIdx(prefixOrIdx);
        } else {
            // If it's not a number we assume prefix
            initSymbolic(prefixOrIdx);
        }
        formula = bvmgr.makeVariable(BIT_WIDTH, name);
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
        formula = bvmgr.makeVariable(BIT_WIDTH, name);
        return name;
    }

    /**
     * Turns this IntValue into a symbolic variable
     *
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC() {
        initSymbolic(symbolicPrefix);
        formula = bvmgr.makeVariable(BIT_WIDTH, name);
        return name;
    }

    /**
     * Creates a formula that asserts that this symbolic value is within the bounds of this type.
     * For BitvectorFormula, bounds are implicit (32-bit), so we return TRUE.
     *
     * @param upper If the upper or lower bound should be created
     * @return The {@link BooleanFormula BooleanFormula} that represents the bounds check (always TRUE)
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        // Bitvector is inherently bounded to 32 bits - no explicit bounds needed
        return context.getFormulaManager().getBooleanFormulaManager().makeTrue();
    }

    /**
     * Creates a formula that asserts that a value is positive
     *
     * @return The {@link BooleanFormula BooleanFormula} that represents the check
     */
    public BooleanFormula checkPositive() {
        return bvmgr.greaterOrEquals(formula, bvmgr.makeBitvector(BIT_WIDTH, 0), true);
    }

    /**
     * Adds two integers. Bitvector addition automatically handles overflow.
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IADD(IntValue i) {
        return new IntValue(context, concrete + i.concrete, bvmgr.add(formula, i.formula));
    }

    /**
     * Subtracts two integers. Bitvector subtraction automatically handles overflow.
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue ISUB(IntValue i) {
        return new IntValue(context, concrete - i.concrete, bvmgr.subtract(formula, i.formula));
    }

    /**
     * Multiplies two integers. Bitvector multiplication automatically handles overflow.
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IMUL(IntValue i) {
        return new IntValue(context, concrete * i.concrete, bvmgr.multiply(formula, i.formula));
    }

    /**
     * Divides two integers using signed bitvector division.
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IDIV(IntValue i) {
        SWATAssert.enforce(i.concrete != 0, "Division by zero!");
        return new IntValue(context, concrete / i.concrete, bvmgr.divide(formula, i.formula, true));
    }

    /**
     * Calculates the remainder of two integers using signed bitvector remainder.
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IREM(IntValue i) {
        return new IntValue(context, concrete % i.concrete, bvmgr.remainder(formula, i.formula, true));
    }

    /**
     * Negates an integer. Bitvector negation automatically handles overflow.
     *
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue INEG() {
        return new IntValue(context, -concrete, bvmgr.negate(formula));
    }

    /**
     * Increments an integer. Bitvector addition automatically handles overflow.
     *
     * @param increment The amount to increment
     * @return The incremented {@link IntValue IntValue}
     */
    public IntValue IINC(int increment) {
        return new IntValue(
                context,
                concrete + increment,
                bvmgr.add(formula, bvmgr.makeBitvector(BIT_WIDTH, increment)));
    }

    /**
     * Calculates the bitwise and of two integers.
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IAND(IntValue i) {
        return new IntValue(context, concrete & i.concrete, bvmgr.and(formula, i.formula));
    }

    /**
     * Calculates the bitwise left shift of an integer.
     *
     * @param i The amount to shift
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue ISHL(IntValue i) {
        // Java spec: shift amount is masked to 5 bits (0-31) for int
        BitvectorFormula shiftAmount = bvmgr.and(i.formula, bvmgr.makeBitvector(BIT_WIDTH, 0b11111));
        return new IntValue(context, concrete << i.concrete, bvmgr.shiftLeft(formula, shiftAmount));
    }

    /**
     * Calculates the bitwise arithmetic right shift of an integer.
     *
     * @param i The amount to shift
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue ISHR(IntValue i) {
        // Java spec: shift amount is masked to 5 bits (0-31) for int
        BitvectorFormula shiftAmount = bvmgr.and(i.formula, bvmgr.makeBitvector(BIT_WIDTH, 0b11111));
        // true = signed/arithmetic shift
        return new IntValue(context, concrete >> i.concrete, bvmgr.shiftRight(formula, shiftAmount, true));
    }

    /**
     * Calculates the bitwise logical right shift of an integer.
     *
     * @param i The amount to shift
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IUSHR(IntValue i) {
        // Java spec: shift amount is masked to 5 bits (0-31) for int
        BitvectorFormula shiftAmount = bvmgr.and(i.formula, bvmgr.makeBitvector(BIT_WIDTH, 0b11111));
        // false = unsigned/logical shift
        return new IntValue(context, concrete >>> i.concrete, bvmgr.shiftRight(formula, shiftAmount, false));
    }

    /**
     * Calculates the bitwise or of two integers.
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IOR(IntValue i) {
        return new IntValue(context, concrete | i.concrete, bvmgr.or(formula, i.formula));
    }

    /**
     * Calculates the bitwise exclusive or of two integers.
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IXOR(IntValue i) {
        return new IntValue(context, concrete ^ i.concrete, bvmgr.xor(formula, i.formula));
    }

    /**
     * Checks if the current Value is 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFEQ() {
        return bvmgr.equal(formula, bvmgr.makeBitvector(BIT_WIDTH, 0));
    }

    /**
     * Checks if the current Value is greater or equal to 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFGE() {
        return bvmgr.greaterOrEquals(formula, bvmgr.makeBitvector(BIT_WIDTH, 0), true);
    }

    /**
     * Checks if the current Value is greater then 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFGT() {
        return bvmgr.greaterThan(formula, bvmgr.makeBitvector(BIT_WIDTH, 0), true);
    }

    /**
     * Checks if the current Value is less or equal to 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFLE() {
        return bvmgr.lessOrEquals(formula, bvmgr.makeBitvector(BIT_WIDTH, 0), true);
    }

    /**
     * Checks if the current Value is greater then 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFLT() {
        return bvmgr.lessThan(formula, bvmgr.makeBitvector(BIT_WIDTH, 0), true);
    }

    /**
     * Checks if the current Value is not equal to 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFNE() {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        return bmgr.not(bvmgr.equal(formula, bvmgr.makeBitvector(BIT_WIDTH, 0)));
    }

    /**
     * Checks if the two integers are equal
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPEQ(IntValue i) {
        return bvmgr.equal(formula, i.formula);
    }

    /**
     * Checks if the integer is greater or equal to the second integer
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPGE(IntValue i) {
        return bvmgr.greaterOrEquals(formula, i.formula, true);
    }

    /**
     * Checks if the integer is greater than the second integer
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPGT(IntValue i) {
        return bvmgr.greaterThan(formula, i.formula, true);
    }

    /**
     * Checks if the integer is less or equal to the second integer
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPLE(IntValue i) {
        return bvmgr.lessOrEquals(formula, i.formula, true);
    }

    /**
     * Checks if the integer is less than the second integer
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPLT(IntValue i) {
        return bvmgr.lessThan(formula, i.formula, true);
    }

    /**
     * Checks if the two integers are not equal
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPNE(IntValue i) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        return bmgr.not(bvmgr.equal(formula, i.formula));
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link DoubleValue DoubleValue}
     *
     * @return The resulting {@link DoubleValue DoubleValue}
     */
    public DoubleValue I2D() {
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                formula,
                true,  // signed
                DoubleValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new DoubleValue(context, (double) concrete, fpFormula);
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link FloatValue FloatValue}
     *
     * @return The resulting {@link FloatValue FloatValue}
     */
    public FloatValue I2F() {
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                formula,
                true,  // signed
                FloatValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new FloatValue(context, (float) concrete, fpFormula);
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link LongValue LongValue}
     *
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue I2L() {
        // Sign-extend 32-bit to 64-bit bitvector
        BitvectorFormula bv64 = bvmgr.extend(formula, 32, true);  // sign extend by 32 bits
        return new LongValue(context, concrete.longValue(), bv64);
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link ByteValue ByteValue}
     *
     * @return The resulting {@link ByteValue ByteValue}
     */
    public ByteValue I2B() {
        // Extract low 8 bits and convert to signed integer
        BitvectorFormula low8 = bvmgr.extract(formula, 7, 0);
        NumeralFormula.IntegerFormula byteFormula = bvmgr.toIntegerFormula(low8, true);
        return new ByteValue(context, concrete.byteValue(), byteFormula);
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link CharValue CharValue}
     *
     * @return The resulting {@link CharValue CharValue}
     */
    public CharValue I2C() {
        // Extract low 16 bits and convert to unsigned integer (char is unsigned)
        BitvectorFormula low16 = bvmgr.extract(formula, 15, 0);
        NumeralFormula.IntegerFormula charFormula = bvmgr.toIntegerFormula(low16, false);
        return new CharValue(context, (char) concrete.intValue(), charFormula);
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link ShortValue ShortValue}
     *
     * @return The resulting {@link ShortValue ShortValue}
     */
    public ShortValue I2S() {
        // Extract low 16 bits and convert to signed integer
        BitvectorFormula low16 = bvmgr.extract(formula, 15, 0);
        NumeralFormula.IntegerFormula shortFormula = bvmgr.toIntegerFormula(low16, true);
        return new ShortValue(context, concrete.shortValue(), shortFormula);
    }

    /**
     * Creates a formula that asserts that a value is not zero.
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula checkZero() {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        return bmgr.not(bvmgr.equal(formula, bvmgr.makeBitvector(BIT_WIDTH, 0)));
    }

    @Override
    public IntValue asIntValue() {
        return this;
    }

    @Override
    public CharValue asCharValue() {
        return I2C();
    }

    @Override
    public BooleanValue asBooleanValue() {
        SWATAssert.enforce(
                concrete == 0 || concrete == 1,
                "Cannot convert {} to a BooleanValue because the concrete value is neither 0 nor 1!", this);
        return new BooleanValue(context, concrete != 0, bvmgr.equal(formula, bvmgr.makeBitvector(BIT_WIDTH, 1)));
    }

    @Override
    public LongValue asLongValue() {
        return I2L();
    }

    @Override
    public ShortValue asShortValue() {
        return I2S();
    }

    @Override
    public ByteValue asByteValue() {
        return I2B();
    }

    @Override
    public FloatValue asFloatValue() {
        return I2F();
    }

    @Override
    public DoubleValue asDoubleValue() {
        return I2D();
    }

    @Override
    public StringValue asStringValue() {
        // Note: Z3's str.from_int only works with non-negative integers (returns "" for negative).
        // To handle negative numbers, we use: ite(i < 0, str.++ "-" (str.from_int |i|), str.from_int i)
        StringFormulaManager sfm = context.getFormulaManager().getStringFormulaManager();
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();

        BitvectorFormula zero32 = bvmgr.makeBitvector(32, 0);

        // Check if value is negative (signed comparison)
        BooleanFormula isNegative = bvmgr.lessThan(formula, zero32, true);

        // Get unsigned interpretation of the bitvector
        NumeralFormula.IntegerFormula unsignedVal = bvmgr.toIntegerFormula(formula, false);

        // For non-negative: just convert unsigned value to string
        StringFormula nonNegStr = sfm.toStringFormula(unsignedVal);

        // For negative: absolute value = 2^32 - unsigned_value, then prepend "-"
        NumeralFormula.IntegerFormula twoTo32 = imgr.makeNumber(4294967296L);
        NumeralFormula.IntegerFormula absVal = imgr.subtract(twoTo32, unsignedVal);
        StringFormula negStr = sfm.concat(sfm.makeString("-"), sfm.toStringFormula(absVal));

        // Combine with if-then-else
        StringFormula resultFormula = bmgr.ifThenElse(isNegative, negStr, nonNegStr);

        return new StringValue(this.context, String.valueOf(concrete), resultFormula, -1);
    }

    @Override
    public IntegerObjectValue asObjectValue() {
        return new IntegerObjectValue(this.context, this, ObjectValue.ADDRESS_UNKNOWN);
    }

    @Override
    public NumericalValue<BitvectorFormula, Integer> asNumericalValue() {
        return this;
    }

    /**
     * Returns the formula as an IntegerFormula for use with array indexing and string operations.
     *
     * @return The formula converted to IntegerFormula
     */
    public NumeralFormula.IntegerFormula asIntegerFormula() {
        return bvmgr.toIntegerFormula(formula, true);
    }

    @Override
    public String getConcreteEncoded() {
        return Integer.toString(concrete);
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
        return genericToString("I");
    }
}
