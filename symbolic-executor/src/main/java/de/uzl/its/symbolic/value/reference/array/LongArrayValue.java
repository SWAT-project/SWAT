package de.uzl.its.symbolic.value.reference.array;

import de.uzl.its.swat.config.Config;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.LongValue;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;

public class LongArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,
                NumeralFormula.IntegerFormula,
                IntValue,
                LongValue,
                long[]> {

    public LongArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.IntegerType, size, address);
        concrete = new long[size.concrete];
        initArray(size.concrete);
    }

    public LongArrayValue(
            SolverContext context,
            IntValue size,
            int address,
            IntValue parentRefIdx,
            ArrayArrayValue parentRef) {
        this(context, size, address);
        this.parentRefIdx = parentRefIdx;
        this.parentRef = parentRef;
    }

    public LongArrayValue(SolverContext context, long[] concrete, int address) {
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
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(0L);
    }

    @Override
    NumeralFormula.IntegerFormula getIndex(int i) {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(i);
    }

    @Override
    public LongValue getElement(IntValue idx) {
        return new LongValue(context, concrete[idx.concrete], amgr.select(formula, idx.formula));
    }

    public void storeElement(IntValue idx, LongValue val) {
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
            concrete[i] = 0L;
        }
    }

    protected void initArray(long[] array) {
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        // ToDo (Nils): Is this needed or correct?
        for (int i = 0; i < array.length; i++) {
            formula = amgr.store(formula, getIndex(i), imgr.makeNumber(concrete[i]));
        }
    }

    @Override
    public LongArrayValue asLongArrayValue() {
        return this;
    }

    /**
     * Returns the string representation of the value used to visualize the stack. The representation is not complete.
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("[J");
    }
}
