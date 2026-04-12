package de.uzl.its.swat.symbolic.value.reference.array;

import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Array value for long arrays, using BitvectorFormula (64-bit) for elements.
 */
public class LongArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,  // Index type (Int)
                BitvectorFormula,                // Element type (64-bit BV)
                IntValue,
                LongValue,
                long[]> {

    private static final int BIT_WIDTH = 64;

    private static final String symbolicPrefix = AbstractArrayValue.getSymbolicArrayPrefix() + LongValue.getSymbolicPrefix();

    public LongArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.getBitvectorTypeWithSize(BIT_WIDTH), symbolicPrefix, size, address);
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
                FormulaType.getBitvectorTypeWithSize(BIT_WIDTH),
                symbolicPrefix,
                new IntValue(context, concrete.length),
                address);
        this.concrete = concrete;
        initArray(concrete);
    }

    @Override
    BitvectorFormula getDefaultValue() {
        return context.getFormulaManager().getBitvectorFormulaManager().makeBitvector(BIT_WIDTH, 0L);
    }

    @Override
    NumeralFormula.IntegerFormula getIndex(int i) {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(i);
    }

    @Override
    public LongValue getElement(IntValue idx) {
        return new LongValue(context, concrete[idx.concrete], amgr.select(formula, idx.asIntegerFormula()));
    }

    public void storeElement(IntValue idx, LongValue val) {
        formula = amgr.store(formula, idx.asIntegerFormula(), val.formula);
        concrete[idx.concrete] = val.concrete;
        if (parentRef != null) {
            parentRef.updateFormula(parentRefIdx, formula);
        }
    }

    @Override
    protected void initArray(int size) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        for (int i = 0; i < size; i++) {
            formula = amgr.store(formula, getIndex(i), bvmgr.makeBitvector(BIT_WIDTH, 0L));
            concrete[i] = 0L;
        }
    }

    protected void initArray(long[] array) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        for (int i = 0; i < array.length; i++) {
            formula = amgr.store(formula, getIndex(i), bvmgr.makeBitvector(BIT_WIDTH, concrete[i]));
        }
    }

    @Override
    public LongArrayValue asLongArrayValue() {
        return this;
    }



    @Override
    public String getSymbolicPrefix() {
        return symbolicPrefix;
    }

    /**
     * Returns the string representation of the value used to visualize the stack. The
     * representation is not complete.
     *
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("[J");
    }
}
