package de.uzl.its.symbolic.value.primitive.numeric.integral;

import de.uzl.its.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.symbolic.value.reference.lang.StringValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for longs to represent symbolic and concrete information on the symbolic stack
 *
 * @author Nils Loose
 * @version 2022.07.18
 */
public class LongValue extends NumericalValue<NumeralFormula.IntegerFormula, Long> {
    private static final String symbolicPrefix = "J";

    /**
     * Creates a LongValue without symbolic information but a concrete value.
     *
     * @param context The SolverContext to create formulas in the native Solver environment.
     * @param concrete The concrete (long) value.
     */
    public LongValue(SolverContext context, long concrete) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = imgr.makeNumber(concrete);
    }

    /**
     * Creates a LongValue with prior symbolic information and a concrete value.
     *
     * @param context The SolverContext to create formulas in the native Solver environment.
     * @param concrete The concrete (long) value.
     * @param formula The formula representing the symbolic information.
     */
    public LongValue(SolverContext context, long concrete, NumeralFormula.IntegerFormula formula) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Turns this LongValue into a symbolic variable
     *
     * @param namePrefix
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(String namePrefix) {
        initSymbolic(namePrefix);
        formula = imgr.makeVariable(name);
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
        formula = imgr.makeVariable(name);
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
        formula = imgr.makeVariable(name);
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
                ? imgr.lessOrEquals(imgr.makeVariable(name), imgr.makeNumber(Long.MAX_VALUE))
                : imgr.greaterOrEquals(imgr.makeVariable(name), imgr.makeNumber(Long.MIN_VALUE));
    }

    /**
     * Adds two longs
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LADD(LongValue l) {
        return new LongValue(
                context, concrete + l.concrete, wrapLong(imgr.add(formula, l.formula)));
    }

    /**
     * Creates a {@link BooleanFormula BooleanFormula} that asserts that a value is not zero
     *
     * @return The resulting {@link BooleanFormula BooleanFormula}
     */
    public BooleanFormula checkZero() {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        return bmgr.not(imgr.equal(formula, imgr.makeNumber(0)));
    }

    /**
     * Calculates the bitwise and of two longs in binary representation
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LAND(LongValue l) {

        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(64, formula);
        BitvectorFormula bv2 = bvmgr.makeBitvector(64, l.formula);
        BitvectorFormula bv3 = bvmgr.and(bv1, bv2);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new LongValue(context, concrete & l.concrete, i1);
    }

    /**
     * Divides two longs
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LDIV(LongValue l) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula cond =
                bmgr.or(
                        imgr.greaterOrEquals(formula, imgr.makeNumber(0)),
                        imgr.equal(imgr.modulo(formula, l.formula), imgr.makeNumber(0)));
        NumeralFormula.IntegerFormula then = imgr.divide(formula, l.formula);

        BooleanFormula innerCond = imgr.greaterOrEquals(l.formula, imgr.makeNumber(0));
        NumeralFormula.IntegerFormula innerThen = imgr.add(then, imgr.makeNumber(1));
        NumeralFormula.IntegerFormula innerElse = imgr.subtract(then, imgr.makeNumber(1));

        NumeralFormula.IntegerFormula elze = bmgr.ifThenElse(innerCond, innerThen, innerElse);
        NumeralFormula.IntegerFormula res = bmgr.ifThenElse(cond, then, elze);
        return new LongValue(context, concrete / l.concrete, wrapLong(res));
    }

    /**
     * Multiplies two longs
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LMUL(LongValue l) {
        return new LongValue(
                context, concrete * l.concrete, wrapLong(imgr.multiply(formula, l.formula)));
    }

    /**
     * Negates a long
     *
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LNEG() {
        return new LongValue(context, -concrete, wrapLong(imgr.negate(formula)));
    }

    /**
     * Calculates the bitwise or of two longs
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LOR(LongValue l) {
        // ToDo (Nils): New symbolic handling needs validation
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(64, formula);
        BitvectorFormula bv2 = bvmgr.makeBitvector(64, l.formula);
        BitvectorFormula bv3 = bvmgr.or(bv1, bv2);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new LongValue(context, concrete | l.concrete, i1);
    }

    /**
     * Calculates the modulo of two longs
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting IntValue
     */
    public LongValue LREM(LongValue l) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula cond =
                bmgr.or(
                        imgr.greaterOrEquals(formula, imgr.makeNumber(0)),
                        imgr.equal(imgr.modulo(formula, l.formula), imgr.makeNumber(0)));
        NumeralFormula.IntegerFormula then = imgr.divide(formula, l.formula);

        BooleanFormula innerCond = imgr.greaterOrEquals(l.formula, imgr.makeNumber(0));
        NumeralFormula.IntegerFormula innerThen = imgr.add(then, imgr.makeNumber(1));
        NumeralFormula.IntegerFormula innerElse = imgr.subtract(then, imgr.makeNumber(1));

        NumeralFormula.IntegerFormula elze = bmgr.ifThenElse(innerCond, innerThen, innerElse);
        NumeralFormula.IntegerFormula truncdiv = bmgr.ifThenElse(cond, then, elze);
        NumeralFormula.IntegerFormula res =
                imgr.subtract(formula, imgr.multiply(truncdiv, l.formula));
        return new LongValue(context, concrete % l.concrete, res);
    }

    /**
     * Calculates the bitwise left shift of a long
     *
     * @param i The amount to shift
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LSHL(IntValue i) {
        // ToDo (Nils): New symbolic handling needs validation
        // Long is shifted by an int --> int converted to 64 instead of 32 bits is that correct?
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(64, formula);
        BitvectorFormula bv2 =
                bvmgr.and(bvmgr.makeBitvector(64, i.formula), bvmgr.makeBitvector(64, 0b111111));

        BitvectorFormula bv3 = bvmgr.shiftLeft(bv1, bv2);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new LongValue(context, concrete << i.concrete, i1);
    }

    /**
     * Calculates the bitwise arithmetic right shift of a long
     *
     * @param i The amount to shift
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LSHR(IntValue i) {
        // ToDo (Nils): New symbolic handling needs validation
        // Long is shifted by an int --> int converted to 64 instead of 32 bits is that correct?
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(64, formula);
        BitvectorFormula bv2 =
                bvmgr.and(bvmgr.makeBitvector(64, i.formula), bvmgr.makeBitvector(64, 0b111111));
        BitvectorFormula bv3 = bvmgr.shiftRight(bv1, bv2, true);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new LongValue(context, concrete >> i.concrete, i1);
    }

    /**
     * Subtracts two longs
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LSUB(LongValue l) {
        return new LongValue(
                context, concrete - l.concrete, wrapLong(imgr.subtract(formula, l.formula)));
    }

    /**
     * Calculates the bitwise logical right shift of a long
     *
     * @param i The amount to shift
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue LUSHR(IntValue i) {
        // ToDo (Nils): New symbolic handling needs validation
        // ToDo (Nils): is shift right signed=false correct?
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(64, formula);
        BitvectorFormula bv2 =
                bvmgr.and(bvmgr.makeBitvector(64, i.formula), bvmgr.makeBitvector(64, 0b111111));

        BitvectorFormula bv3 = bvmgr.shiftRight(bv1, bv2, false);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new LongValue(context, concrete >>> i.concrete, i1);
    }

    /**
     * Calculates the bitwise exclusive or of two longs
     *
     * @param l The other {@link LongValue LongValue}
     * @return The resulting IntValue
     */
    public LongValue LXOR(LongValue l) {
        // ToDo (Nils): New symbolic handling needs validation
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(64, formula);
        BitvectorFormula bv2 = bvmgr.makeBitvector(64, l.formula);
        BitvectorFormula bv3 = bvmgr.xor(bv1, bv2);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new LongValue(context, concrete ^ l.concrete, i1);
    }

    /**
     * Compares two longs and returns: 0 if both values are equal 1 if this long is greater -1 if
     * the parameter is greater ToDo (Nils): This method is not validated
     *
     * @param l The other {@link LongValue LongValue}Value
     * @return The resulting IntValue with the boolean conditions
     */
    public IntValue LCMP(LongValue l) {
        int c = concrete.compareTo(l.concrete);

        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        BooleanFormula eqCond = imgr.equal(formula, l.formula);
        BooleanFormula gtCond = imgr.greaterThan(formula, l.formula);
        NumeralFormula.IntegerFormula tmp =
                bmgr.ifThenElse(gtCond, imgr.makeNumber(1), imgr.makeNumber(-1));
        NumeralFormula.IntegerFormula res = bmgr.ifThenElse(eqCond, imgr.makeNumber(0), tmp);

        return new IntValue(context, c, res);
    }

    /**
     * Converts a long into a double
     *
     * @return The resulting DoubleValue
     */
    public DoubleValue L2D() {
        return new DoubleValue(
                context,
                concrete.doubleValue(),
                context.getFormulaManager()
                        .getFloatingPointFormulaManager()
                        .castFrom(formula, true, DoubleValue.precision));
    }

    /**
     * Converts a long into a float
     *
     * @return The resulting FloatValue
     */
    public FloatValue L2F() {
        return new FloatValue(
                context,
                concrete.floatValue(),
                context.getFormulaManager()
                        .getFloatingPointFormulaManager()
                        .castFrom(formula, true, FloatValue.precision));
    }

    /**
     * Converts a long into an int
     *
     * @return The resulting IntValue
     */
    public IntValue L2I() {
        // ToDo (Nils): Currently long and int are treated equivalently, is there a better way?
        return new IntValue(context, concrete.intValue(), wrapInteger(formula));
    }

    public LongValue asLongValue() {
        return this;
    }

    @Override
    public StringValue asStringValue() {
        return new StringValue(
                this.context,
                String.valueOf(concrete),
                context.getFormulaManager().getStringFormulaManager().toStringFormula(this.formula),
                -1);
    }

    @Override
    public String getConcreteEncoded() {
        return Long.toString(concrete);
    }

    /**
     * Returns the string representation of the value used to visualize the stack. The representation is not complete.
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("J");
    }
}
