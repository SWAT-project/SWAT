package de.uzl.its.symbolic.value.reference.array;

import de.uzl.its.swat.config.Config;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import org.sosy_lab.java_smt.api.*;

public class FloatArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,
                FloatingPointFormula,
                IntValue,
                FloatValue,
                float[]> {

    public FloatArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FloatValue.precision, size, address);
        concrete = new float[size.concrete];
        initArray(size.concrete);
    }

    public FloatArrayValue(
            SolverContext context,
            IntValue size,
            int address,
            IntValue parentRefIdx,
            ArrayArrayValue parentRef) {
        this(context, size, address);
        this.parentRefIdx = parentRefIdx;
        this.parentRef = parentRef;
    }

    public FloatArrayValue(SolverContext context, float[] concrete, int address) {
        super(
                context,
                FormulaType.IntegerType,
                FloatValue.precision,
                new IntValue(context, concrete.length),
                address);
        this.concrete = concrete;
        initArray(concrete);
    }

    @Override
    FloatingPointFormula getDefaultValue() {
        return context.getFormulaManager()
                .getFloatingPointFormulaManager()
                .makeNumber(0.0, FloatValue.precision);
    }

    @Override
    NumeralFormula.IntegerFormula getIndex(int i) {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(i);
    }

    @Override
    public FloatValue getElement(IntValue idx) {
        return new FloatValue(context, concrete[idx.concrete], amgr.select(formula, idx.formula));
    }

    @Override
    public void storeElement(IntValue idx, FloatValue val) {
        formula = amgr.store(formula, idx.formula, val.formula);
        concrete[idx.concrete] = val.concrete;
        if (parentRef != null) {
            parentRef.updateFormula(parentRefIdx, formula);
        }
    }

    @Override
    protected void initArray(int size) {
        // ToDo (Nils): Is this needed or correct?
        for (int i = 0; i < size; i++) {
            formula = amgr.store(formula, getIndex(i), getDefaultValue());
            concrete[i] = 0.0f;
        }
    }

    protected void initArray(float[] array) {
        FloatingPointFormulaManager fmgr =
                context.getFormulaManager().getFloatingPointFormulaManager();

        for (int i = 0; i < array.length; i++) {
            formula =
                    amgr.store(
                            formula,
                            getIndex(i),
                            fmgr.makeNumber(concrete[i], FloatValue.precision));
        }
    }

    @Override
    public FloatArrayValue asFloatArrayValue() {
        return this;
    }

    /**
     * Returns the string representation of the value used to visualize the stack. The representation is not complete.
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("[F");
    }
}
