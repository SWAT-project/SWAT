package de.uzl.its.swat.symbolic.value.primitive.numeric;

import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.symbolic.value.Value;
import org.sosy_lab.java_smt.api.*;

/**
 * @param <T> The type of Formula used by this Value
 * @param <K> The class wrapper around the primitive datatype this value represents
 */
public abstract class NumericalValue<T extends Formula, K> extends Value<T, K> {

    /** Java-smt formula manager for handling integral formulas */
    protected IntegerFormulaManager imgr;

    /**
     * Models int overflow in constraints
     *
     * @param i The formula to wrap
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapInteger(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Integer.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Integer.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Integer.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Integer.MIN_VALUE)));
    }

    /**
     * Models long overflow in constraints
     *
     * @param i The formula to wrapLong
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapLong(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Long.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Long.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Long.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Long.MIN_VALUE)));
    }

    /**
     * Models short overflow in constraints
     *
     * @param i The formula to wrap
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapShort(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Short.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Short.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Short.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Short.MIN_VALUE)));
    }

    /**
     * Models short overflow in constraints
     *
     * @param i The formula to wrap
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapByte(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Byte.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Byte.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Byte.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Byte.MIN_VALUE)));
    }

    /**
     * Models short overflow in constraints
     *
     * @param i The formula to wrap
     * @return The wrapped formula
     */
    protected NumeralFormula.IntegerFormula wrapCharacter(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(
                                i,
                                imgr.multiply(
                                        imgr.makeNumber(-1), imgr.makeNumber(Character.MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(Character.MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(Character.MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(Character.MIN_VALUE)));
    }

    /**
     * Converts an integer formula to floating-point formula using BitVector intermediate.
     * This avoids the uninterpreted function issue in JavaSMT when casting Integer → FP directly.
     *
     * Uses IEEE 754 round-to-nearest-even (RNE) mode, which is Java's default rounding mode
     * for int/long to float/double conversions per JLS 5.1.2.
     *
     * @param intFormula The integer formula to convert
     * @param sourceBitWidth The source bit width (32 for int, 64 for long)
     * @param targetType The target floating-point type (float or double precision)
     * @return The resulting floating-point formula
     */
    protected FloatingPointFormula intToFpFormula(
            NumeralFormula.IntegerFormula intFormula,
            int sourceBitWidth,
            FormulaType.FloatingPointType targetType) {

        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();

        // Step 1: Convert integer to signed bitvector
        BitvectorFormula bv = bvmgr.makeBitvector(sourceBitWidth, intFormula);

        // Step 2: Cast bitvector to floating-point using RNE rounding (Java's default)
        FloatingPointFormula fpFormula = fpmgr.castFrom(
            bv,
            true,  // signed
            targetType,
            FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN
        );

        return fpFormula;
    }

    /**
     * Converts a floating-point formula to integer formula using BitVector intermediate.
     * This avoids the uninterpreted function issue in JavaSMT when casting FP → Integer directly (castTo).
     *
     * Uses IEEE 754 round-toward-zero (RTZ) mode.in line with JVM Spec 2.8.
     * Special cases:
     * - NaN → 0
     * - +Infinity → INT_MAX (or LONG_MAX)
     * - -Infinity → INT_MIN (or LONG_MIN)
     * - Out of range → clamped to INT_MAX/MIN (or LONG_MAX/MIN)
     *
     * @param fpFormula The floating-point formula to convert
     * @param bitWidth The target bit width (32 for int, 64 for long)
     * @return The resulting integer formula
     */
    protected NumeralFormula.IntegerFormula fpToIntFormula(
            FloatingPointFormula fpFormula,
            int bitWidth) {

        FloatingPointFormulaManager fpmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();

        // Cast FP to signed bitvector using RTZ rounding
        BitvectorFormula bv = fpmgr.castTo(
            fpFormula,
            true,  // signed
            FormulaType.getBitvectorTypeWithSize(bitWidth),
            FloatingPointRoundingMode.TOWARD_ZERO
        );

        // Convert bitvector to integer (signed)
        NumeralFormula.IntegerFormula intFormula = bvmgr.toIntegerFormula(bv, true);

        // Step 3: Handle special cases (NaN, ±Infinity, out-of-range)

        BooleanFormula isNaN = fpmgr.isNaN(fpFormula);

        // Check for positive infinity
        BooleanFormula isPosInf = bmgr.and(
            fpmgr.isInfinity(fpFormula),
            bmgr.not(fpmgr.isNegative(fpFormula))
        );

        // Check for negative infinity
        BooleanFormula isNegInf = bmgr.and(
            fpmgr.isInfinity(fpFormula),
            fpmgr.isNegative(fpFormula)
        );

        // Determine max/min values based on bit width
        long maxValue = (bitWidth == 32) ? Integer.MAX_VALUE : Long.MAX_VALUE;
        long minValue = (bitWidth == 32) ? Integer.MIN_VALUE : Long.MIN_VALUE;

        // Apply JVM-specified defaults for special cases
        NumeralFormula.IntegerFormula result = bmgr.ifThenElse(
            isNaN,
            imgr.makeNumber(0),  // NaN → 0
            bmgr.ifThenElse(
                isPosInf,
                imgr.makeNumber(maxValue),  // +Infinity → MAX_VALUE
                bmgr.ifThenElse(
                    isNegInf,
                    imgr.makeNumber(minValue),  // -Infinity → MIN_VALUE
                    intFormula  // Normal case: use the bitvector conversion
                )
            )
        );

        return result;
    }


    @Override
    public boolean isSymbolic() {
        return !context.getFormulaManager().extractVariables(formula).isEmpty();
    }

    @Override
    public ImmutableSet<String> getSymbolicVariables() {
        return context.getFormulaManager().extractVariables(formula).keySet();
    }


    public String genericToString(String type) {
        String formulaString = this.formula.toString();
        if (formulaString.length() > Config.instance().getLoggingFormulaLength()) {
            formulaString =
                    formulaString.substring(0, Config.instance().getLoggingFormulaLength()) + "...";
        }
        return type + "[" + this.internalID + "] (" + concrete + ", " + formulaString + ")";
    }
}
