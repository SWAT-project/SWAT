package de.uzl.its.swat.symbolic.value.reference.array;

import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.BooleanValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for Arrays that contain boolean values.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class BooleanArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula, BooleanFormula, IntValue, BooleanValue, boolean[]> {

    /**
     * Creates a new symbolic array that contains boolean values
     *
     * @param context The SolverContext to create formulas in the native Solver environment.
     * @param size The size of the array
     * @param address The address of the array reference
     */
    public BooleanArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.BooleanType, size, address);
        concrete = new boolean[size.concrete];
        initArray(size.concrete);
    }

    public BooleanArrayValue(
            SolverContext context,
            IntValue size,
            int address,
            IntValue parentRefIdx,
            ArrayArrayValue parentRef) {
        this(context, size, address);
        this.parentRefIdx = parentRefIdx;
        this.parentRef = parentRef;
    }

    public BooleanArrayValue(SolverContext context, boolean[] concrete, int address) {
        super(
                context,
                FormulaType.IntegerType,
                FormulaType.BooleanType,
                new IntValue(context, concrete.length),
                address);
        this.concrete = concrete;
        initArray(concrete);
    }

    protected void initArray(boolean[] array) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        // ToDo (Nils): Is this needed or correct?
        for (int i = 0; i < array.length; i++) {
            formula = amgr.store(formula, getIndex(i), bmgr.makeBoolean(concrete[i]));
        }
    }
    /**
     * Returns the default value for the type of array.
     *
     * @return The default value for the type of array.
     */
    @Override
    BooleanFormula getDefaultValue() {
        return context.getFormulaManager().getBooleanFormulaManager().makeBoolean(false);
    }

    /**
     * Returns a formula representing the index specified by i.
     *
     * @param i The integer representation (concrete value) of the index.
     * @return The formula representing the index.
     */
    @Override
    NumeralFormula.IntegerFormula getIndex(int i) {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(i);
    }

    /**
     * Returns the formula that represents the element at the specified index.
     *
     * @param idx The formula that specifies the index.
     * @return The formula that specifies the retrieved/ retrievable element.
     */
    @Override
    public BooleanValue getElement(IntValue idx) {
        return new BooleanValue(context, concrete[idx.concrete], amgr.select(formula, idx.formula));
    }

    /**
     * Stores the formula representing an element at the position specified by the index formula.
     *
     * @param idx The index formula.
     * @param val The element formula.
     */
    @Override
    public void storeElement(IntValue idx, BooleanValue val) {
        formula = amgr.store(formula, idx.formula, val.formula);
        concrete[idx.concrete] = val.concrete;

        if (parentRef != null) {
            parentRef.updateFormula(parentRefIdx, formula);
        }
    }

    /**
     * Initializes the array with the default value.
     *
     * @param size The size of the array.
     */
    @Override
    protected void initArray(int size) {
        // ToDo (Nils): Is this needed or correct?
        for (int i = 0; i < size; i++) {
            formula = amgr.store(formula, getIndex(i), getDefaultValue());
            concrete[i] = false;
        }
    }

    @Override
    public BooleanArrayValue asBooleanArrayValue() {
        return this;
    }

    /**
     * Returns the string representation of the value used to visualize the stack. The
     * representation is not complete.
     *
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("[Z");
    }
}
