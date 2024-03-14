package de.uzl.its.swat.symbolic.value.primitive.numeric.integral;

import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.sosy_lab.java_smt.api.*;

/**
 * IntValue contains a pair of concrete value and a path constraint. Note that the path constraint
 * flip the boolean signs according to the evaluated concrete value.
 */
public class IntValue extends NumericalValue<NumeralFormula.IntegerFormula, Integer> {
    private static final String symbolicPrefix = "I";

    /**
     * Instantiates a new Int value.
     *
     * @param context the context
     * @param concrete the concrete
     */
    public IntValue(SolverContext context, int concrete) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = imgr.makeNumber(concrete);
    }

    /**
     * Instantiates a new Int value.
     *
     * @param context the context
     * @param concrete the concrete
     * @param formula the formula
     */
    public IntValue(SolverContext context, int concrete, NumeralFormula.IntegerFormula formula) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Turns this IntValue into a symbolic variable
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
     * Turns this IntValue into a symbolic variable
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
     * Creates a formula that asserts that this symbolic value is withing the bounds of this type
     *
     * @param upper If the upper or lower bound should be created
     * @return The {@link BooleanFormula BooleanFormula} that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        return upper
                ? imgr.lessOrEquals(imgr.makeVariable(name), imgr.makeNumber(Integer.MAX_VALUE))
                : imgr.greaterOrEquals(imgr.makeVariable(name), imgr.makeNumber(Integer.MIN_VALUE));
    }

    /**
     * Creates a formula that asserts that a value is positive
     *
     * @return The {@link BooleanFormula BooleanFormula} that represents the check
     */
    public BooleanFormula checkPositive() {
        return imgr.greaterOrEquals(formula, imgr.makeNumber(0));
    }

    /**
     * Adds two integers
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IADD(IntValue i) {
        return new IntValue(
                context, concrete + i.concrete, wrapInteger(imgr.add(formula, i.formula)));
    }

    /**
     * Subtracts two integers
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue ISUB(IntValue i) {
        return new IntValue(
                context, concrete - i.concrete, wrapInteger(imgr.subtract(formula, i.formula)));
    }

    /**
     * Multiplies two integers
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IMUL(IntValue i) {
        return new IntValue(
                context, concrete * i.concrete, wrapInteger(imgr.multiply(formula, i.formula)));
    }

    /**
     * Divides two integers
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IDIV(IntValue i) {
        assert i.concrete != 0 : "Division by zero!";
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula cond =
                bmgr.or(
                        imgr.greaterOrEquals(formula, imgr.makeNumber(0)),
                        imgr.equal(imgr.modulo(formula, i.formula), imgr.makeNumber(0)));
        NumeralFormula.IntegerFormula then = imgr.divide(formula, i.formula);

        BooleanFormula innerCond = imgr.greaterOrEquals(i.formula, imgr.makeNumber(0));
        NumeralFormula.IntegerFormula innerThen = imgr.add(then, imgr.makeNumber(1));
        NumeralFormula.IntegerFormula innerElse = imgr.subtract(then, imgr.makeNumber(1));

        NumeralFormula.IntegerFormula elze = bmgr.ifThenElse(innerCond, innerThen, innerElse);
        NumeralFormula.IntegerFormula res = bmgr.ifThenElse(cond, then, elze);
        return new IntValue(context, concrete / i.concrete, wrapInteger(res));
    }

    /**
     * Calculates the modulo of two integers
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IREM(IntValue i) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula cond =
                bmgr.or(
                        imgr.greaterOrEquals(formula, imgr.makeNumber(0)),
                        imgr.equal(imgr.modulo(formula, i.formula), imgr.makeNumber(0)));
        NumeralFormula.IntegerFormula then = imgr.divide(formula, i.formula);

        BooleanFormula innerCond = imgr.greaterOrEquals(i.formula, imgr.makeNumber(0));
        NumeralFormula.IntegerFormula innerThen = imgr.add(then, imgr.makeNumber(1));
        NumeralFormula.IntegerFormula innerElse = imgr.subtract(then, imgr.makeNumber(1));

        NumeralFormula.IntegerFormula elze = bmgr.ifThenElse(innerCond, innerThen, innerElse);
        NumeralFormula.IntegerFormula truncdiv = bmgr.ifThenElse(cond, then, elze);
        NumeralFormula.IntegerFormula res =
                imgr.subtract(formula, imgr.multiply(truncdiv, i.formula));
        return new IntValue(context, concrete % i.concrete, res);
    }

    /**
     * Negates an integer
     *
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue INEG() {
        return new IntValue(context, -1 * concrete, wrapInteger(imgr.negate(formula)));
    }

    /**
     * Increments an integer
     *
     * @param increment The amount to increment
     * @return The incremented {@link IntValue IntValue}
     */
    public IntValue IINC(int increment) {
        return new IntValue(
                context,
                concrete + increment,
                wrapInteger(imgr.add(formula, imgr.makeNumber(increment))));
    }

    /**
     * Calculates the bitwise and of two integers in binary representation
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IAND(IntValue i) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(32, formula);
        BitvectorFormula bv2 = bvmgr.makeBitvector(32, i.formula);
        BitvectorFormula bv3 = bvmgr.and(bv1, bv2);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new IntValue(context, concrete & i.concrete, i1);
    }

    /**
     * Calculates the bitwise left shift of an integer
     *
     * @param i The amount to shift
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue ISHL(IntValue i) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(32, formula);
        BitvectorFormula bv2 =
                bvmgr.and(bvmgr.makeBitvector(32, i.formula), bvmgr.makeBitvector(32, 0b11111));
        BitvectorFormula bv3 = bvmgr.shiftLeft(bv1, bv2);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new IntValue(context, concrete << i.concrete, i1);
    }

    /**
     * Calculates the bitwise arithmetic right shift of an integer
     *
     * @param i The amount to shift
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue ISHR(IntValue i) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(32, formula);
        BitvectorFormula bv2 =
                bvmgr.and(bvmgr.makeBitvector(32, i.formula), bvmgr.makeBitvector(32, 0b11111));
        BitvectorFormula bv3 = bvmgr.shiftRight(bv1, bv2, true);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new IntValue(context, concrete >> i.concrete, i1);
    }

    /**
     * Calculates the bitwise logical right shift of an integer
     *
     * @param i The amount to shift
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IUSHR(IntValue i) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(32, formula);
        BitvectorFormula bv2 =
                bvmgr.and(bvmgr.makeBitvector(32, i.formula), bvmgr.makeBitvector(32, 0b11111));
        BitvectorFormula bv3 = bvmgr.shiftRight(bv1, bv2, false);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new IntValue(context, concrete >>> i.concrete, i1);
    }

    /**
     * Calculates the bitwise or of two integers
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IOR(IntValue i) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(32, formula);
        BitvectorFormula bv2 = bvmgr.makeBitvector(32, i.formula);
        BitvectorFormula bv3 = bvmgr.or(bv1, bv2);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new IntValue(context, concrete | i.concrete, i1);
    }

    /**
     * Calculates the bitwise exclusive or of two integers
     *
     * @param i The other {@link IntValue IntValue}
     * @return The resulting {@link IntValue IntValue}
     */
    public IntValue IXOR(IntValue i) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        BitvectorFormula bv1 = bvmgr.makeBitvector(32, formula);
        BitvectorFormula bv2 = bvmgr.makeBitvector(32, i.formula);
        BitvectorFormula bv3 = bvmgr.xor(bv1, bv2);
        NumeralFormula.IntegerFormula i1 = bvmgr.toIntegerFormula(bv3, true);
        return new IntValue(context, concrete ^ i.concrete, i1);
    }

    /**
     * Checks if the current Value is 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFEQ() {
        return imgr.equal(formula, imgr.makeNumber(0));
    }

    /**
     * Checks if the current Value is greater or equal to 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFGE() {
        return imgr.greaterOrEquals(formula, imgr.makeNumber(0));
    }

    /**
     * Checks if the current Value is greater then 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFGT() {
        return imgr.greaterThan(formula, imgr.makeNumber(0));
    }

    /**
     * Checks if the current Value is less or equal to 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFLE() {
        return imgr.lessOrEquals(formula, imgr.makeNumber(0));
    }

    /**
     * Checks if the current Value is greater then 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFLT() {
        return imgr.lessThan(formula, imgr.makeNumber(0));
    }

    /**
     * Checks if the current Value is not equal to 0
     *
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IFNE() {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        return bmgr.not(imgr.equal(formula, imgr.makeNumber(0)));
    }

    /**
     * Checks if the two integers are equal
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPEQ(IntValue i) {
        return imgr.equal(formula, i.formula);
    }

    /**
     * Checks if the integer is greater or equal to the second integer
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPGE(IntValue i) {
        return imgr.greaterOrEquals(formula, i.formula);
    }

    /**
     * Checks if the integer is greater than the second integer
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPGT(IntValue i) {
        return imgr.greaterThan(formula, i.formula);
    }

    /**
     * Checks if the integer is less or equal to the second integer
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPLE(IntValue i) {
        return imgr.lessOrEquals(formula, i.formula);
    }

    /**
     * Checks if the integer is less than the second integer
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPLT(IntValue i) {
        return imgr.lessThan(formula, i.formula);
    }

    /**
     * Checks if the two integers are not equal
     *
     * @param i The other {@link IntValue IntValue}
     * @return A {@link BooleanFormula BooleanFormula} that represents this check
     */
    public BooleanFormula IF_ICMPNE(IntValue i) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        return bmgr.not(imgr.equal(formula, i.formula));
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link DoubleValue DoubleValue}
     *
     * @return The resulting {@link DoubleValue DoubleValue}
     */
    public DoubleValue I2D() {
        return new DoubleValue(
                context,
                (double) concrete,
                context.getFormulaManager()
                        .getFloatingPointFormulaManager()
                        .castFrom(formula, true, DoubleValue.precision));
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link FloatValue FloatValue}
     *
     * @return The resulting {@link FloatValue FloatValue}
     */
    public FloatValue I2F() {
        return new FloatValue(
                context,
                (float) concrete,
                context.getFormulaManager()
                        .getFloatingPointFormulaManager()
                        .castFrom(formula, true, FloatValue.precision));
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link LongValue LongValue}
     *
     * @return The resulting {@link LongValue LongValue}
     */
    public LongValue I2L() {
        return new LongValue(context, concrete.longValue(), formula);
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link ByteValue ByteValue}
     *
     * @return The resulting {@link ByteValue ByteValue}
     */
    public ByteValue I2B() {
        return new ByteValue(context, concrete.byteValue(), wrapByte(formula));
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link CharValue CharValue} ToDo (Nils):
     * Symbolic information is lost! See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/60">Issue
     * 60</a>
     *
     * @return The resulting {@link CharValue CharValue}
     */
    public CharValue I2C() {
        return new CharValue(context, (char) concrete.intValue(), wrapCharacter(formula));
    }

    /**
     * Converts an {@link IntValue IntValue} into a {@link ShortValue ShortValue}
     *
     * @return The resulting {@link ShortValue ShortValue}
     */
    public ShortValue I2S() {
        return new ShortValue(context, concrete.shortValue(), wrapShort(formula));
    }

    public BooleanFormula checkZero() {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        return bmgr.not(imgr.equal(formula, imgr.makeNumber(0)));
    }

    @Override
    public IntValue asIntValue() {
        return this;
    }

    @Override
    public CharValue asCharValue() {
        // ToDo (Nils): What if int is out of bounds?
        return I2C();
    }

    @Override
    public BooleanValue asBooleanValue() {
        // ToDo (Nils): Formula creation not verified
        if (concrete != 0 && concrete != 1)
            throw new RuntimeException(
                    "Cannot convert: "
                            + this
                            + " to a BooleanValue because the concrete value is neither 0 nor 1!");
        return new BooleanValue(context, concrete != 0, imgr.equal(formula, imgr.makeNumber(1)));
    }

    @Override
    public LongValue asLongValue() {
        return new LongValue(this.context, this.concrete.longValue(), this.formula);
    }

    @Override
    public ShortValue asShortValue() {
        // ToDo: No restrictions for range of short in formula
        return new ShortValue(this.context, this.concrete.shortValue(), this.formula);
    }

    @Override
    public ByteValue asByteValue() {
        // ToDo: No restrictions for range of short in formula
        return new ByteValue(this.context, this.concrete.byteValue(), this.formula);
    }

    @Override
    public StringValue asStringValue() {
        // ToDo (Nils): Not validated
        return new StringValue(
                this.context,
                String.valueOf(concrete),
                context.getFormulaManager().getStringFormulaManager().toStringFormula(this.formula),
                -1);
    }

    @Override
    public NumericalValue<NumeralFormula.IntegerFormula, Integer> asNumericalValue() {
        return this;
    }

    @Override
    public String getConcreteEncoded() {
        return Integer.toString(concrete);
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
