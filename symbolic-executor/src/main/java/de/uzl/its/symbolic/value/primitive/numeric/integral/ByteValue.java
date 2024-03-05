package de.uzl.its.symbolic.value.primitive.numeric.integral;

import de.uzl.its.symbolic.value.primitive.numeric.NumericalValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper to represent byte values on the symbolic stack. Can contain concrete and symbolic
 * information.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class ByteValue extends NumericalValue<NumeralFormula.IntegerFormula, Byte> {
    private static final String symbolicPrefix = "B";

    /**
     * Creates a new ByteValue that has no prior symbolic information and only contains a specific
     * value
     *
     * @param context The SolverContext
     * @param concrete The concrete byte value
     */
    public ByteValue(SolverContext context, byte concrete) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = imgr.makeNumber(concrete);
    }

    /**
     * Creates a new ByteValue that has prior symbolic information and contains a specific value
     *
     * @param context The SolverContext
     * @param concrete The concrete byte value
     * @param formula The symbolic formula representing prior symbolic information about this byte
     */
    public ByteValue(SolverContext context, byte concrete, NumeralFormula.IntegerFormula formula) {
        this.context = context;
        this.imgr = context.getFormulaManager().getIntegerFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
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
                ? imgr.lessOrEquals(imgr.makeVariable(name), imgr.makeNumber(Byte.MAX_VALUE))
                : imgr.greaterOrEquals(imgr.makeVariable(name), imgr.makeNumber(Byte.MIN_VALUE));
    }
    /**
     * Turns this ByteValue into a symbolic variable
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
     * Turns this ByteValue into a symbolic variable
     *
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC() {
        initSymbolic(symbolicPrefix);
        formula = imgr.makeVariable(name);
        return name;
    }

    @Override
    public NumericalValue<NumeralFormula.IntegerFormula, Byte> asNumericalValue() {
        return this;
    }

    @Override
    public IntValue asIntValue() {
        return new IntValue(context, concrete, formula);
    }

    @Override
    public ByteValue asByteValue() {
        return this;
    }

    @Override
    public String getConcreteEncoded() {
        return Byte.toString(concrete);
    }


    /**
     * Returns the string representation of the value used to visualize the stack. The representation is not complete.
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("B");
    }
}
