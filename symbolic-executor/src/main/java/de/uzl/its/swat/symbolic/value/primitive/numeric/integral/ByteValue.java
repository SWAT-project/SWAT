package de.uzl.its.swat.symbolic.value.primitive.numeric.integral;

import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.ByteObjectValue;
import lombok.Getter;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper to represent byte values (8-bit signed) on the symbolic stack.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class ByteValue extends NumericalValue<BitvectorFormula, Byte> {
    @Getter
    private static final String symbolicPrefix = "B";

    /** Bit width for byte values */
    private static final int BIT_WIDTH = 8;

    /** Bitvector formula manager for handling 8-bit bitvector formulas */
    protected BitvectorFormulaManager bvmgr;

    /**
     * Creates a new ByteValue that has no prior symbolic information and only contains a specific
     * value
     *
     * @param context The SolverContext
     * @param concrete The concrete byte value
     */
    public ByteValue(SolverContext context, byte concrete) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, concrete);
    }

    /**
     * Creates a new ByteValue that has prior symbolic information and contains a specific value
     *
     * @param context The SolverContext
     * @param concrete The concrete byte value
     * @param formula The symbolic bitvector formula representing prior symbolic information
     */
    public ByteValue(SolverContext context, byte concrete, BitvectorFormula formula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Creates a new ByteValue from an IntegerFormula (for backward compatibility).
     * Converts the IntegerFormula to BitvectorFormula.
     *
     * @param context The SolverContext
     * @param concrete The concrete byte value
     * @param intFormula The integer formula to convert
     */
    public ByteValue(SolverContext context, byte concrete, NumeralFormula.IntegerFormula intFormula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        // Convert IntegerFormula to BitvectorFormula
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, intFormula);
    }

    /**
     * Creates a formula that asserts that this symbolic value is within the bounds of this type.
     * For BitvectorFormula, bounds are implicit (8-bit), so we return TRUE.
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check (always TRUE)
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        // Bitvector is inherently bounded to 8 bits - no explicit bounds needed
        return context.getFormulaManager().getBooleanFormulaManager().makeTrue();
    }

    /**
     * Turns this ByteValue into a symbolic variable
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
     * Turns this ByteValue into a symbolic variable
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
     * Turns this ByteValue into a symbolic variable
     *
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC() {
        initSymbolic(symbolicPrefix);
        formula = bvmgr.makeVariable(BIT_WIDTH, name);
        return name;
    }

    @Override
    public NumericalValue<BitvectorFormula, Byte> asNumericalValue() {
        return this;
    }

    @Override
    public IntValue asIntValue() {
        // byte → int: sign-extend 8-bit to 32-bit bitvector
        BitvectorFormula bv32 = bvmgr.extend(formula, 24, true);  // sign extend by 24 bits
        return new IntValue(context, (int) concrete, bv32);
    }

    @Override
    public LongValue asLongValue() {
        // byte → long: sign-extend 8-bit to 64-bit bitvector
        BitvectorFormula bv64 = bvmgr.extend(formula, 56, true);  // sign extend by 56 bits
        return new LongValue(context, concrete.longValue(), bv64);
    }

    @Override
    public ByteValue asByteValue() {
        return this;
    }

    @Override
    public ShortValue asShortValue() {
        // byte → short: sign-extend 8-bit to 16-bit bitvector
        BitvectorFormula bv16 = bvmgr.extend(formula, 8, true);  // sign extend by 8 bits
        return new ShortValue(context, concrete.shortValue(), bv16);
    }

    /**
     * Converts this ByteValue to a CharValue.
     * byte → char: per JLS, first widens to int, then narrows to char (unsigned 16-bit)
     *
     * @return The resulting CharValue
     */
    @Override
    public CharValue asCharValue() {
        char charVal = (char) concrete.byteValue();
        // byte → char: sign-extend to 16-bit (same bit pattern as short, but interpreted as unsigned)
        BitvectorFormula bv16 = bvmgr.extend(formula, 8, true);  // sign extend by 8 bits
        return new CharValue(context, charVal, bv16);
    }

    @Override
    public FloatValue asFloatValue() {
        // byte → float: widening conversion
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        // First sign-extend to 32-bit, then convert to FP
        BitvectorFormula bv32 = bvmgr.extend(formula, 24, true);
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                bv32,
                true,  // signed
                FloatValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new FloatValue(context, concrete.floatValue(), fpFormula);
    }

    @Override
    public DoubleValue asDoubleValue() {
        // byte → double: widening conversion
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        // First sign-extend to 32-bit, then convert to FP
        BitvectorFormula bv32 = bvmgr.extend(formula, 24, true);
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                bv32,
                true,  // signed
                DoubleValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new DoubleValue(context, concrete.doubleValue(), fpFormula);
    }

    @Override
    public ByteObjectValue asObjectValue() {
        return new ByteObjectValue(context, this, ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Returns the formula as an IntegerFormula for use with operations that require it.
     *
     * @return The formula converted to IntegerFormula
     */
    public NumeralFormula.IntegerFormula asIntegerFormula() {
        return bvmgr.toIntegerFormula(formula, true);
    }

    @Override
    public String getConcreteEncoded() {
        return Byte.toString(concrete);
    }

    @Override
    public String getSymPrefix() {
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
        return genericToString("B");
    }
}
