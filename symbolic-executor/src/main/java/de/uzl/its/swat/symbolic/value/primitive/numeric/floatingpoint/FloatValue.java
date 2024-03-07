package de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint;

import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.sosy_lab.java_smt.api.*;

public class FloatValue extends NumericalValue<FloatingPointFormula, Float> {
    private static final String symbolicPrefix = "F";

    public static final FormulaType.FloatingPointType precision =
            FormulaType.getSinglePrecisionFloatingPointType();
    private FloatingPointFormulaManager fmgr;

    public FloatValue(SolverContext context, float concrete) {
        this.context = context;
        this.fmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        this.concrete = concrete;
        // ToDo (Nils): What rounding strategy does java use. It should be reflected here
        this.formula = fmgr.makeNumber(concrete, precision);
    }

    public FloatValue(SolverContext context, float concrete, FloatingPointFormula formula) {
        this.context = context;
        this.fmgr = context.getFormulaManager().getFloatingPointFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Turns this DoubleValue into a symbolic variable
     *
     * @param namePrefix
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(String namePrefix) {
        initSymbolic(namePrefix);
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
     * Creates a formula that asserts that this symbolic value is withing the bounds of this type
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        return upper
                ? fmgr.lessOrEquals(
                        fmgr.makeVariable(name, precision),
                        fmgr.makeNumber(Float.MAX_VALUE, precision))
                : fmgr.greaterOrEquals(
                        fmgr.makeVariable(name, precision),
                        fmgr.makeNumber(-Float.MAX_VALUE, precision));
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
     * Casts a float to an integer
     *
     * @return The resulting IntValue
     */
    public IntValue F2I() {
        // ToDo (Nils): Choose a rounding strategy?
        // ToDo (Nils): determine the correct sign bit here!
        return new IntValue(
                context, concrete.intValue(), fmgr.castTo(formula, true, FormulaType.IntegerType));
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
     * Casts a float to a long
     *
     * @return The resulting IntValue
     */
    public LongValue F2L() {
        // ToDo (Nils): Choose a rounding strategy?
        return new LongValue(
                context, concrete.longValue(), fmgr.castTo(formula, true, FormulaType.IntegerType));
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

    @Override
    public FloatValue asFloatValue() {
        return this;
    }

    @Override
    public StringValue asStringValue() {
        return new StringValue(context, String.valueOf(context), -1);
    }

    @Override
    public String getConcreteEncoded() {
        int intBits = Float.floatToIntBits(concrete);
        return Integer.toHexString(intBits);
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
