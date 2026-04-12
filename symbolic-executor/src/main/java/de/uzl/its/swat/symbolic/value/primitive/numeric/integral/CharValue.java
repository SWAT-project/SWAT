package de.uzl.its.swat.symbolic.value.primitive.numeric.integral;

import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.CharacterObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import lombok.Getter;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper to represent char values (16-bit unsigned) on the symbolic stack.
 */
public class CharValue extends NumericalValue<BitvectorFormula, Character> {
    @Getter
    private static final String symbolicPrefix = "C";

    /** Bit width for char values */
    private static final int BIT_WIDTH = 16;

    /** Bitvector formula manager for handling 16-bit bitvector formulas */
    protected BitvectorFormulaManager bvmgr;

    /**
     * Creates a new CharValue that has no prior symbolic information and only contains a specific
     * value
     *
     * @param context The SolverContext
     * @param concrete The concrete char value
     */
    public CharValue(SolverContext context, char concrete) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, concrete);
    }

    /**
     * Creates a new CharValue that has prior symbolic information from a StringFormula.
     *
     * @param context The SolverContext
     * @param concrete The concrete char value
     * @param stringFormula The string formula to convert to a char code point
     */
    public CharValue(SolverContext context, char concrete, StringFormula stringFormula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        // Use str.to_code to convert single-character string to code point, then to BV
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        NumeralFormula.IntegerFormula intFormula = smgr.toCodePoint(stringFormula);
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, intFormula);
    }

    /**
     * Creates a new CharValue that has prior symbolic information and contains a specific value
     *
     * @param context The SolverContext
     * @param concrete The concrete char value
     * @param formula The symbolic bitvector formula representing prior symbolic information
     */
    public CharValue(SolverContext context, char concrete, BitvectorFormula formula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Creates a new CharValue from an IntegerFormula (for backward compatibility).
     * Converts the IntegerFormula to BitvectorFormula.
     *
     * @param context The SolverContext
     * @param concrete The concrete char value
     * @param intFormula The integer formula to convert
     */
    public CharValue(SolverContext context, char concrete, NumeralFormula.IntegerFormula intFormula) {
        this.context = context;
        this.bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        // Convert IntegerFormula to BitvectorFormula
        this.formula = bvmgr.makeBitvector(BIT_WIDTH, intFormula);
    }

    /**
     * Creates a formula that asserts that this symbolic value is within the bounds of this type.
     * For BitvectorFormula, bounds are implicit (16-bit unsigned), so we return TRUE.
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check (always TRUE)
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        // Bitvector is inherently bounded to 16 bits - no explicit bounds needed
        return context.getFormulaManager().getBooleanFormulaManager().makeTrue();
    }

    /**
     * Turns this CharValue into a symbolic variable
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
     * Turns this CharValue into a symbolic variable
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
     * Turns this CharValue into a symbolic variable
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
     * Converts this CharValue to a ByteValue using implicit narrowing conversion.
     * Per JVM Spec 2.11.4 char → byte (discard all but lowest 8 bits)
     *
     * @return The resulting ByteValue
     */
    @Override
    public ByteValue asByteValue() {
        byte byteVal = (byte) concrete.charValue();
        // Extract low 8 bits from 16-bit bitvector
        BitvectorFormula bv8 = bvmgr.extract(formula, 7, 0);
        return new ByteValue(context, byteVal, bv8);
    }

    /**
     * Converts this CharValue to a ShortValue using implicit narrowing conversion.
     * Per JVM Spec 2.11.4 char → short (reinterpret bits as signed)
     *
     * @return The resulting ShortValue
     */
    @Override
    public ShortValue asShortValue() {
        short shortVal = (short) concrete.charValue();
        // Same bits, just reinterpreted as signed
        return new ShortValue(context, shortVal, formula);
    }

    @Override
    public CharValue asCharValue() {
        return this;
    }

    @Override
    public IntValue asIntValue() {
        // char → int: zero-extend 16-bit to 32-bit bitvector (char is unsigned)
        BitvectorFormula bv32 = bvmgr.extend(formula, 16, false);  // zero extend by 16 bits
        return new IntValue(context, (int) concrete.charValue(), bv32);
    }

    @Override
    public LongValue asLongValue() {
        // char → long: zero-extend 16-bit to 64-bit bitvector (char is unsigned)
        BitvectorFormula bv64 = bvmgr.extend(formula, 48, false);  // zero extend by 48 bits
        return new LongValue(context, (long) concrete.charValue(), bv64);
    }

    @Override
    public FloatValue asFloatValue() {
        // char → float: widening conversion (char is unsigned 16-bit)
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        // First zero-extend to 32-bit, then convert to FP
        BitvectorFormula bv32 = bvmgr.extend(formula, 16, false);  // zero extend (unsigned)
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                bv32,
                false,  // unsigned - char is unsigned
                FloatValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new FloatValue(context, (float) concrete.charValue(), fpFormula);
    }

    @Override
    public DoubleValue asDoubleValue() {
        // char → double: widening conversion (char is unsigned 16-bit)
        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        // First zero-extend to 32-bit, then convert to FP
        BitvectorFormula bv32 = bvmgr.extend(formula, 16, false);  // zero extend (unsigned)
        FloatingPointFormula fpFormula = fpmgr.castFrom(
                bv32,
                false,  // unsigned - char is unsigned
                DoubleValue.precision,
                FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
        return new DoubleValue(context, (double) concrete.charValue(), fpFormula);
    }

    @Override
    public StringValue asStringValue() {
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        // Convert BV to IntegerFormula for string operations
        NumeralFormula.IntegerFormula intFormula = bvmgr.toIntegerFormula(formula, false);  // unsigned
        return new StringValue(
                context,
                String.valueOf(concrete),
                smgr.fromCodePoint(intFormula),
                -1);
    }

    @Override
    public CharacterObjectValue asObjectValue() {
        return new CharacterObjectValue(context, this, ObjectValue.ADDRESS_UNKNOWN);
    }

    /**
     * Returns the formula as an IntegerFormula for use with operations that require it.
     * Note: char is unsigned, so we use unsigned conversion.
     *
     * @return The formula converted to IntegerFormula
     */
    public NumeralFormula.IntegerFormula asIntegerFormula() {
        return bvmgr.toIntegerFormula(formula, false);  // unsigned
    }

    @Override
    public String getConcreteEncoded() {
        return Character.toString(concrete);
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
        return genericToString("C");
    }
}
