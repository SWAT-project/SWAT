package de.uzl.its.symbolic.value.primitive.numeric.integral;

import de.uzl.its.symbolic.value.primitive.numeric.NumericalValue;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Wrapper for shorts to represent symbolic and concrete information on the symbolic stack
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class ShortValue extends NumericalValue<NumeralFormula.IntegerFormula, Short> {

    private static int MIN_VALUE = Short.MIN_VALUE;
    private static int MAX_VALUE = Short.MAX_VALUE;
    private static final String symbolicPrefix = "S";

    /**
     * Creates a new ShortValue that has no prior symbolic information and only contains a specific
     * value
     *
     * @param context The SolverContext
     * @param concrete The concrete short value
     */
    public ShortValue(SolverContext context, short concrete) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = imgr.makeNumber(concrete);
    }

    /**
     * Creates a new ShortValue that has prior symbolic information and contains a specific value
     *
     * @param context The SolverContext
     * @param concrete The concrete short value
     * @param formula The symbolic formula representing prior symbolic information about this short
     */
    public ShortValue(
            SolverContext context, short concrete, NumeralFormula.IntegerFormula formula) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Turns this ShortValue into a symbolic variable
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
     * Turns this ShortValue into a symbolic variable
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
                ? imgr.lessOrEquals(imgr.makeVariable(name), imgr.makeNumber(Short.MAX_VALUE))
                : imgr.greaterOrEquals(imgr.makeVariable(name), imgr.makeNumber(Short.MIN_VALUE));
    }

    private NumeralFormula.IntegerFormula wrap(NumeralFormula.IntegerFormula i) {
        return imgr.subtract(
                imgr.modulo(
                        imgr.add(i, imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(MIN_VALUE))),
                        imgr.subtract(
                                imgr.add(imgr.makeNumber(MAX_VALUE), imgr.makeNumber(1)),
                                imgr.makeNumber(MIN_VALUE))),
                imgr.multiply(imgr.makeNumber(-1), imgr.makeNumber(MIN_VALUE)));
    }

    @Override
    public NumericalValue<NumeralFormula.IntegerFormula, Short> asNumericalValue() {
        return this;
    }

    @Override
    public ShortValue asShortValue() {
        return this;
    }

    @Override
    public IntValue asIntValue() {
        return new IntValue(context, concrete, formula);
    }

    @Override
    public String getConcreteEncoded() {
        return Short.toString(concrete);
    }

    /**
     * Returns the string representation of the value used to visualize the stack. The representation is not complete.
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("S");
    }
}
