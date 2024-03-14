package de.uzl.its.swat.symbolic.value.primitive.numeric.integral;

import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper to represent boolean values on the symbolic stack. Can contain concrete and symbolic
 * information.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class BooleanValue extends NumericalValue<BooleanFormula, Boolean> {
    private static final String symbolicPrefix = "Z";

    /** Java-smt formula manager for handling boolean formulas */
    private BooleanFormulaManager bmgr;

    /**
     * Creates a new BooleanValue that has no prior symbolic information and only contains a
     * specific value
     *
     * @param context The SolverContext
     * @param concrete The concrete boolean value
     */
    public BooleanValue(SolverContext context, boolean concrete) {
        this.context = context;
        this.bmgr = context.getFormulaManager().getBooleanFormulaManager();
        this.concrete = concrete;
        this.formula = bmgr.makeBoolean(concrete);
    }

    /**
     * Creates a new BooleanValue that has prior symbolic information and contains a specific value
     *
     * @param context The SolverContext
     * @param concrete The concrete boolean value
     * @param formula The symbolic formula representing prior symbolic information about this
     *     boolean
     */
    public BooleanValue(SolverContext context, boolean concrete, BooleanFormula formula) {
        this.context = context;
        this.bmgr = context.getFormulaManager().getBooleanFormulaManager();
        this.concrete = concrete;
        this.formula = formula;
    }

    /**
     * Turns this BooleanValue into a symbolic variable
     *
     * @param namePrefix
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(String namePrefix) {
        initSymbolic(namePrefix);
        formula = bmgr.makeVariable(name);
        return name;
    }

    /**
     * Turns this BooleanValue into a symbolic variable
     *
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC() {
        initSymbolic(symbolicPrefix);
        formula = bmgr.makeVariable(name);
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
        formula = bmgr.makeVariable(name);
        return name;
    }

    @Override
    public NumericalValue<BooleanFormula, Boolean> asNumericalValue() {
        return this;
    }
    /**
     * Creates a formula that asserts that this symbolic value is withing the bounds of this type
     *
     * @param upper If the upper or lower bound should be created
     * @return The BooleanFormula that represents the bounds check
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        return context.getFormulaManager().getBooleanFormulaManager().makeBoolean(true);
    }

    @Override
    public IntValue asIntValue() {
        imgr = context.getFormulaManager().getIntegerFormulaManager();
        return new IntValue(
                context,
                concrete ? 1 : 0,
                bmgr.ifThenElse(formula, imgr.makeNumber(1), imgr.makeNumber(0)));
    }

    @Override
    public BooleanValue asBooleanValue() {
        return this;
    }

    @Override
    public StringValue asStringValue() {
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        return new StringValue(
                context,
                String.valueOf(concrete),
                bmgr.ifThenElse(formula, smgr.makeString("true"), smgr.makeString("false")),
                ObjectValue.ADDRESS_UNKNOWN);
    }

    @Override
    public String getConcreteEncoded() {
        return Boolean.toString(concrete);
    }
    /**
     * Returns the string representation of the value used to visualize the stack. The
     * representation is not complete.
     *
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("Z");
    }
}
