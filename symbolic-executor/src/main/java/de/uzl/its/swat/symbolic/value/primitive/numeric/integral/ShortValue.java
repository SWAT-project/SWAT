package de.uzl.its.swat.symbolic.value.primitive.numeric.integral;

import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.ShortObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import lombok.Getter;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper to represent short values (16-bit signed) on the symbolic stack.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class ShortValue extends NumericalValue<BitvectorFormula, Short> {

    @Getter
    private static final String symbolicPrefix = "S";

    /** Bit width for short values */
    private static final int BIT_WIDTH = 16;

    /** Bitvector formula manager for handling 16-bit bitvector formulas */
    protected BitvectorFormulaManager bvmgr;

    /**
     * Creates a new ShortValue that has no prior symbolic information and only contains a specific
     * value
     *
     * @param context The SolverContext
     * @param concrete The concrete short value
     */
    public ShortValue(SolverContext context, short concrete) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, concrete);
    }

    /**
     * Creates a new ShortValue that has prior symbolic information and contains a specific value
     *
     * @param context The SolverContext
     * @param concrete The concrete short value
     * @param formula The symbolic bitvector formula representing prior symbolic information
     */
    public ShortValue(SolverContext context, short concrete, BitvectorFormula formula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Creates a new ShortValue from an IntegerFormula (for backward compatibility).
     * Converts the IntegerFormula to BitvectorFormula.
     *
     * @param context The SolverContext
     * @param concrete The concrete short value
     * @param intFormula The integer formula to convert
     */
    public ShortValue(SolverContext context, short concrete, NumeralFormula.IntegerFormula intFormula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        // Convert IntegerFormula to BitvectorFormula
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, intFormula);
    }

    /**
     * Turns this ShortValue into a symbolic variable
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
     * Turns this ShortValue into a symbolic variable
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
     * Turns this ShortValue into a symbolic variable
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
     * For BitvectorFormula, bounds are implicit (16-bit), so we return TRUE.
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check (always TRUE)
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        // Bitvector is inherently bounded to 16 bits - no explicit bounds needed
        return context.getFormulaManager().getBooleanFormulaManager().makeTrue();
    }

    @Override
    public NumericalValue<BitvectorFormula, Short> asNumericalValue() {
        return this;
    }

    @Override
    public ShortValue asShortValue() {
        return this;
    }

    /**
     * Converts this ShortValue to a ByteValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: short -> byte
     *
     * @return The resulting ByteValue
     */
    @Override
    public ByteValue asByteValue() {
        byte byteVal = (byte) concrete.shortValue();
        // Extract low 8 bits from 16-bit bitvector
        BitvectorFormula bv8 = bvmgr.extract(formula, 7, 0);
        return new ByteValue(context, byteVal, bv8);
    }

    /**
     * Converts this ShortValue to a CharValue using implicit narrowing conversion.
     * Per JVM Spec 2.8: short -> char (reinterpret as unsigned 16-bit)
     *
     * @return The resulting CharValue
     */
    @Override
    public CharValue asCharValue() {
        char charVal = (char) concrete.shortValue();
        // Same bits, just reinterpreted as unsigned
        return new CharValue(context, charVal, formula);
    }

    @Override
    public IntValue asIntValue() {
        // short → int: sign-extend 16-bit to 32-bit bitvector
        BitvectorFormula bv32 = bvmgr.extend(formula, 16, true);  // sign extend by 16 bits
        return new IntValue(context, (int) concrete, bv32);
    }

    @Override
    public LongValue asLongValue() {
        // short → long: sign-extend 16-bit to 64-bit bitvector
        BitvectorFormula bv64 = bvmgr.extend(formula, 48, true);  // sign extend by 48 bits
        return new LongValue(context, concrete.longValue(), bv64);
    }

    @Override
    public FloatValue asFloatValue() {
        // short → float: widening conversion
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        // First sign-extend to 32-bit, then convert to FP
        BitvectorFormula bv32 = bvmgr.extend(formula, 16, true);
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                bv32,
                true,  // signed
                FloatValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new FloatValue(context, concrete.floatValue(), fpFormula);
    }

    @Override
    public DoubleValue asDoubleValue() {
        // short → double: widening conversion
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        // First sign-extend to 32-bit, then convert to FP
        BitvectorFormula bv32 = bvmgr.extend(formula, 16, true);
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                bv32,
                true,  // signed
                DoubleValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new DoubleValue(context, concrete.doubleValue(), fpFormula);
    }

    @Override
    public ShortObjectValue asObjectValue() {
        return new ShortObjectValue(context, this, ObjectValue.ADDRESS_UNKNOWN);
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
        return Short.toString(concrete);
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
        return genericToString("S");
    }
}
