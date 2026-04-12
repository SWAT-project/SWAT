package de.uzl.its.swat.symbolic.value.reference.array;

import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for Arrays that contain integer values.
 * Uses BitvectorFormula (32-bit) for elements.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class IntArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,  // Index type (Int)
                BitvectorFormula,                // Element type (32-bit BV)
                IntValue,
                IntValue,
                int[]> {

    private static final int BIT_WIDTH = 32;


    private static final String symbolicPrefix = AbstractArrayValue.getSymbolicArrayPrefix() + IntValue.getSymbolicPrefix();

    public IntArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.getBitvectorTypeWithSize(BIT_WIDTH), symbolicPrefix, size, address);
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
                FormulaType.getBitvectorTypeWithSize(BIT_WIDTH),
                symbolicPrefix,
                new IntValue(context, concrete.length),
                address);
        this.concrete = concrete;
        initArray(concrete);
    }

    @Override
    BitvectorFormula getDefaultValue() {
        return context.getFormulaManager().getBitvectorFormulaManager().makeBitvector(BIT_WIDTH, 0);
    }

    @Override
    NumeralFormula.IntegerFormula getIndex(int i) {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(i);
    }

    @Override
    public IntValue getElement(IntValue idx) {
        return new IntValue(context, concrete[idx.concrete], amgr.select(formula, idx.asIntegerFormula()));
    }

    public void storeElement(IntValue idx, IntValue val) {
        concrete[idx.concrete] = val.concrete;
        formula = amgr.store(formula, idx.asIntegerFormula(), val.formula);

        if (parentRef != null) {
            parentRef.updateFormula(parentRefIdx, formula);
        }
    }

    @Override
    protected void initArray(int size) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        for (int i = 0; i < size; i++) {
            formula = amgr.store(formula, getIndex(i), bvmgr.makeBitvector(BIT_WIDTH, 0));
            concrete[i] = 0;
        }
    }

    protected void initArray(int[] array) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        for (int i = 0; i < array.length; i++) {
            formula = amgr.store(formula, getIndex(i), bvmgr.makeBitvector(BIT_WIDTH, concrete[i]));
        }
    }

    @Override
    public IntArrayValue asIntArrayValue() {
        return this;
    }

    @Override
    public AbstractArrayValue<
                    NumeralFormula.IntegerFormula,
                    BitvectorFormula,
                    IntValue,
                    IntValue,
                    int[]>
            asArrayValue() {
        return this;
    }



    @Override
    public String getSymPrefix() {
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
        return genericToString("[I");
    }
}
