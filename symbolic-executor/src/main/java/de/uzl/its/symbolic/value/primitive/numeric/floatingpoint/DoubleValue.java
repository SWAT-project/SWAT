package de.uzl.its.symbolic.value.primitive.numeric.floatingpoint;

import de.uzl.its.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.symbolic.value.reference.lang.StringValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for doubles to represent symbolic and concrete information on the symbolic stack
 *
 * @author Nils Loose
 * @version 2022.07.09
 */
public class DoubleValue extends NumericalValue<FloatingPointFormula, Double> {
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
                        fmgr.makeNumber(Double.MAX_VALUE, precision))
                : fmgr.greaterOrEquals(
                        fmgr.makeVariable(name, precision),
                        fmgr.makeNumber(-Double.MAX_VALUE, precision));
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
     * Compares two doubles and returns: 0 if both values are equal 1 if this double is greater or
     * either float is NaN -1 if the parameter is greater ToDo (Nils): This method is not validated
     *
     * @param f The other DoubleValue
     * @return The resulting IntValue with the boolean conditions
     */
    public IntValue DCMPG(DoubleValue f) {
        if (concrete.isNaN() || f.concrete.isNaN()) {
            return new IntValue(context, 1);
        }
        return DCMP(f);
    }

    /**
     * Compares two doubles and returns: 0 if both values are equal 1 if this double is greater -1
     * if the parameter is greater or either float is NaN ToDo (Nils): This method is not validated
     *
     * @param f The other DoubleValue
     * @return The resulting IntValue with the boolean conditions
     */
    public IntValue DCMPL(DoubleValue f) {
        if (concrete.isNaN() || f.concrete.isNaN()) {
            return new IntValue(context, -1);
        }
        return DCMP(f);
    }

    /**
     * Compares two doubles and returns: 0 if both values are equal 1 if this double is greater -1
     * if the parameter is greater
     *
     * @param f The other DoubleValue
     * @return The resulting IntValue with the boolean conditions
     */
    private IntValue DCMP(DoubleValue f) {
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
     * Casts a double to an integer
     *
     * @return The resulting IntValue
     */
    public IntValue D2I() {
        // ToDo (Nils): Choose a rounding strategy?
        return new IntValue(
                context, concrete.intValue(), fmgr.castTo(formula, true, FormulaType.IntegerType));
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
     * Casts a double to a long
     *
     * @return The resulting LongValue
     */
    public LongValue D2L() {
        // ToDo (Nils): Choose a rounding strategy?
        return new LongValue(
                context, concrete.longValue(), fmgr.castTo(formula, true, FormulaType.IntegerType));
    }

    @Override
    public String getConcreteEncoded() {
        long longBits = Double.doubleToLongBits(concrete);
        return Long.toHexString(longBits);
    }

    @Override
    public DoubleValue asDoubleValue() {
        return this;
    }

    @Override
    public StringValue asStringValue() {
        return new StringValue(context, String.valueOf(context), -1);
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
