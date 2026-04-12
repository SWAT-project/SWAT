package de.uzl.its.swat.symbolic.value.primitive.numeric.integral;

import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.LongObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import lombok.Getter;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper to represent long values (64-bit signed) on the symbolic stack.
 *
 * @author Nils Loose
 * @version 2022.07.18
 */
public class LongValue extends NumericalValue<BitvectorFormula, Long> {
    @Getter
    private static final String symbolicPrefix = "J";

    /** Bit width for long values */
    private static final int BIT_WIDTH = 64;

    /** Bitvector formula manager for handling 64-bit bitvector formulas */
    protected BitvectorFormulaManager bvmgr;

    /**
     * Creates a LongValue without symbolic information but a concrete value.
     *
     * @param context The SolverContext to create formulas in the native Solver environment.
     * @param concrete The concrete (long) value.
     */
    public LongValue(SolverContext context, long concrete) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, concrete);
    }

    /**
     * Creates a LongValue with prior symbolic information and a concrete value.
     *
     * @param context The SolverContext to create formulas in the native Solver environment.
     * @param concrete The concrete (long) value.
     * @param formula The bitvector formula representing the symbolic information.
     */
    public LongValue(SolverContext context, long concrete, BitvectorFormula formula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Turns this LongValue into a symbolic variable
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
        formula = bvmgr.makeVariable(BIT_WIDTH, name);
        return name;
    }

    /**
     * Turns this LongValue into a symbolic variable
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
     * Turns this LongValue into a symbolic variable
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
     * Creates a formula that asserts that this symbolic value is within the bounds of this type.
     * For BitvectorFormula, bounds are implicit (64-bit), so we return TRUE.
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check (always TRUE for bitvectors)
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        // Bitvector is inherently bounded to 64 bits - no explicit bounds needed
        return context.getFormulaManager().getBooleanFormulaManager().makeTrue();
    }

    /**
     * Adds two longs. Bitvector addition automatically handles overflow.
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LADD(LongValue l) {
        return new LongValue(context, concrete + l.concrete, bvmgr.add(formula, l.formula));
    }

    /**
     * Creates a {@link BooleanFormula BooleanFormula} that asserts that a value is not zero
     *
     * @return The resulting {@link BooleanFormula BooleanFormula}
     */
    public BooleanFormula checkZero() {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        return bmgr.not(bvmgr.equal(formula, bvmgr.makeBitvector(BIT_WIDTH, 0)));
    }

    /**
     * Calculates the bitwise and of two longs.
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LAND(LongValue l) {
        return new LongValue(context, concrete & l.concrete, bvmgr.and(formula, l.formula));
    }

    /**
     * Divides two longs using signed bitvector division.
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LDIV(LongValue l) {
        // Use signed bitvector division
        return new LongValue(context, concrete / l.concrete, bvmgr.divide(formula, l.formula, true));
    }

    /**
     * Multiplies two longs. Bitvector multiplication automatically handles overflow.
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LMUL(LongValue l) {
        return new LongValue(context, concrete * l.concrete, bvmgr.multiply(formula, l.formula));
    }

    /**
     * Negates a long. Bitvector negation automatically handles overflow.
     *
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LNEG() {
        return new LongValue(context, -concrete, bvmgr.negate(formula));
    }

    /**
     * Calculates the bitwise or of two longs.
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LOR(LongValue l) {
        return new LongValue(context, concrete | l.concrete, bvmgr.or(formula, l.formula));
    }

    /**
     * Calculates the remainder of two longs using signed bitvector remainder.
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting LongValue
     */
    public LongValue LREM(LongValue l) {
        return new LongValue(context, concrete % l.concrete, bvmgr.remainder(formula, l.formula, true));
    }

    /**
     * Calculates the bitwise left shift of a long.
     *
     * @param i The amount to shift
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LSHL(IntValue i) {
        // Java spec: shift amount is masked to 6 bits (0-63) for long
        // IntValue is now 32-bit BV, extend to 64-bit for shift operation
        BitvectorFormula shiftBv64 = bvmgr.extend(i.formula, 32, false); // zero-extend
        BitvectorFormula shiftAmount = bvmgr.and(shiftBv64, bvmgr.makeBitvector(BIT_WIDTH, 0b111111));
        return new LongValue(context, concrete << i.concrete, bvmgr.shiftLeft(formula, shiftAmount));
    }

    /**
     * Calculates the bitwise arithmetic right shift of a long.
     *
     * @param i The amount to shift
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LSHR(IntValue i) {
        // Java spec: shift amount is masked to 6 bits (0-63) for long
        // IntValue is now 32-bit BV, extend to 64-bit for shift operation
        BitvectorFormula shiftBv64 = bvmgr.extend(i.formula, 32, false); // zero-extend
        BitvectorFormula shiftAmount = bvmgr.and(shiftBv64, bvmgr.makeBitvector(BIT_WIDTH, 0b111111));
        // true = signed/arithmetic shift
        return new LongValue(context, concrete >> i.concrete, bvmgr.shiftRight(formula, shiftAmount, true));
    }

    /**
     * Subtracts two longs. Bitvector subtraction automatically handles overflow.
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LSUB(LongValue l) {
        return new LongValue(context, concrete - l.concrete, bvmgr.subtract(formula, l.formula));
    }

    /**
     * Calculates the bitwise logical right shift of a long.
     *
     * @param i The amount to shift
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LUSHR(IntValue i) {
        // Java spec: shift amount is masked to 6 bits (0-63) for long
        // IntValue is now 32-bit BV, extend to 64-bit for shift operation
        BitvectorFormula shiftBv64 = bvmgr.extend(i.formula, 32, false); // zero-extend
        BitvectorFormula shiftAmount = bvmgr.and(shiftBv64, bvmgr.makeBitvector(BIT_WIDTH, 0b111111));
        // false = unsigned/logical shift
        return new LongValue(context, concrete >>> i.concrete, bvmgr.shiftRight(formula, shiftAmount, false));
    }

    /**
     * Calculates the bitwise exclusive or of two longs.
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting LongValue
     */
    public LongValue LXOR(LongValue l) {
        return new LongValue(context, concrete ^ l.concrete, bvmgr.xor(formula, l.formula));
    }

    /**
     * Compares two longs and returns: 0 if both values are equal 1 if this long is greater -1 if
     * the parameter is greater.
     *
     * @param l The other {@link LongValue LongValue}Value
     * @return The resulting IntValue with the boolean conditions
     */
    public IntValue LCMP(LongValue l) {
        int c = concrete.compareTo(l.concrete);

        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();

        // Use signed comparison for bitvectors
        BooleanFormula eqCond = bvmgr.equal(formula, l.formula);
        BooleanFormula gtCond = bvmgr.greaterThan(formula, l.formula, true); // true = signed

        NumeralFormula.IntegerFormula tmp =
                bmgr.ifThenElse(gtCond, imgr.makeNumber(1), imgr.makeNumber(-1));
        NumeralFormula.IntegerFormula res = bmgr.ifThenElse(eqCond, imgr.makeNumber(0), tmp);

        return new IntValue(context, c, res);
    }

    /**
     * Converts a long into a double.
     *
     * @return The resulting DoubleValue
     */
    public DoubleValue L2D() {
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        // Direct bitvector to floating-point conversion using RNE rounding (Java's default)
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                formula,
                true,  // signed
                DoubleValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new DoubleValue(context, concrete.doubleValue(), fpFormula);
    }

    /**
     * Converts a long into a float.
     *
     * @return The resulting FloatValue
     */
    public FloatValue L2F() {
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        // Direct bitvector to floating-point conversion using RNE rounding (Java's default)
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                formula,
                true,  // signed
                FloatValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new FloatValue(context, concrete.floatValue(), fpFormula);
    }

    /**
     * Converts a long into an int.
     *
     * @return The resulting IntValue
     */
    public IntValue L2I() {
        // Extract low 32 bits - result is already a BitvectorFormula
        BitvectorFormula low32 = bvmgr.extract(formula, 31, 0);
        return new IntValue(context, concrete.intValue(), low32);
    }

    /**
     * Converts this LongValue to a ByteValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: long -> int -> byte (two-step narrowing)
     * Extracts the low 8 bits from the bitvector.
     *
     * @return The resulting ByteValue
     */
    @Override
    public ByteValue asByteValue() {
        // Extract low 8 bits and convert to signed integer
        BitvectorFormula low8 = bvmgr.extract(formula, 7, 0);
        NumeralFormula.IntegerFormula byteFormula = bvmgr.toIntegerFormula(low8, true);
        return new ByteValue(context, (byte) concrete.intValue(), byteFormula);
    }

    /**
     * Converts this LongValue to a ShortValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: long -> int -> short (two-step narrowing)
     * Extracts the low 16 bits from the bitvector.
     *
     * @return The resulting ShortValue
     */
    @Override
    public ShortValue asShortValue() {
        // Extract low 16 bits and convert to signed integer
        BitvectorFormula low16 = bvmgr.extract(formula, 15, 0);
        NumeralFormula.IntegerFormula shortFormula = bvmgr.toIntegerFormula(low16, true);
        return new ShortValue(context, (short) concrete.intValue(), shortFormula);
    }

    /**
     * Converts this LongValue to a CharValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: long -> int -> char (two-step narrowing)
     * Extracts the low 16 bits from the bitvector (unsigned).
     *
     * @return The resulting CharValue
     */
    @Override
    public CharValue asCharValue() {
        // Extract low 16 bits and convert to unsigned integer (char is unsigned)
        BitvectorFormula low16 = bvmgr.extract(formula, 15, 0);
        NumeralFormula.IntegerFormula charFormula = bvmgr.toIntegerFormula(low16, false);
        return new CharValue(context, (char) concrete.intValue(), charFormula);
    }

    /**
     * Returns this value as a LongValue (identity conversion).
     *
     * @return This LongValue
     */
    @Override
    public LongValue asLongValue() {
        return this;
    }

    /**
     * Returns this value as an IntValue.
     *
     * @return The corresponding IntValue
     */
    @Override
    public IntValue asIntValue() {
        return L2I();
    }

    @Override
    public FloatValue asFloatValue() {
        return L2F();
    }

    @Override
    public DoubleValue asDoubleValue() {
        return L2D();
    }

    @Override
    public StringValue asStringValue() {
        // String theory requires IntegerFormula, so convert BV to Int
        NumeralFormula.IntegerFormula intFormula = bvmgr.toIntegerFormula(formula, true);
        return new StringValue(
                this.context,
                String.valueOf(concrete),
                context.getFormulaManager().getStringFormulaManager().toStringFormula(intFormula),
                -1);
    }

    @Override
    public LongObjectValue asObjectValue(){
        return new LongObjectValue(context, this, ObjectValue.ADDRESS_UNKNOWN);
    }

    @Override
    public String getConcreteEncoded() {
        return Long.toString(concrete);
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
        return genericToString("J");
    }
}
