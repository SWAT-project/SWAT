package de.uzl.its.symbolic.value.reference.array;

import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for Arrays that contain integer values.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class IntArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,
                NumeralFormula.IntegerFormula,
                IntValue,
                IntValue,
                int[]> {

    public IntArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.IntegerType, size, address);
        concrete = new int[size.concrete];
        initArray(size.concrete);
    }

    public IntArrayValue(
            SolverContext context,
            IntValue size,
            int address,
            IntValue parentRefIdx,
            ArrayArrayValue parentRef) {
        this(context, size, address);
        this.parentRefIdx = parentRefIdx;
        this.parentRef = parentRef;
    }

    public IntArrayValue(SolverContext context, int[] concrete, int address) {
        super(
                context,
                FormulaType.IntegerType,
                FormulaType.IntegerType,
                new IntValue(context, concrete.length),
                address);
        this.concrete = concrete;
        initArray(concrete);
    }

    @Override
    NumeralFormula.IntegerFormula getDefaultValue() {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(0);
    }

    @Override
    NumeralFormula.IntegerFormula getIndex(int i) {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(i);
    }

    @Override
    public IntValue getElement(IntValue idx) {
        return new IntValue(context, concrete[idx.concrete], amgr.select(formula, idx.formula));
    }

    public void storeElement(IntValue idx, IntValue val) {
        concrete[idx.concrete] = val.concrete;
        formula = amgr.store(formula, idx.formula, val.formula);

        if (parentRef != null) {
            parentRef.updateFormula(parentRefIdx, formula);
        }
    }

    @Override
    protected void initArray(int size) {
        // ToDo (Nils): Is this needed or correct?
        // For Multinewarrays it seems needed as rthe values are else free i.e. arbitrary
        for (int i = 0; i < size; i++) {
            formula = amgr.store(formula, getIndex(i), getDefaultValue());
            concrete[i] = 0;
        }
    }

    protected void initArray(int[] array) {
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        // ToDo (Nils): Is this needed or correct?
        for (int i = 0; i < array.length; i++) {
            formula = amgr.store(formula, getIndex(i), imgr.makeNumber(concrete[i]));
        }
    }

    @Override
    public IntArrayValue asIntArrayValue() {
        return this;
    }

    @Override
    public AbstractArrayValue<
                    NumeralFormula.IntegerFormula,
                    NumeralFormula.IntegerFormula,
                    IntValue,
                    IntValue,
                    int[]>
            asArrayValue() {
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
        return genericToString("[I");
    }
}
