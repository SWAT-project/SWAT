package de.uzl.its.swat.symbolic.value.reference.array;

import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.ShortValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for Arrays that contain short values.
 * Uses BitvectorFormula (16-bit) for elements.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class ShortArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,  // Index type (Int)
                BitvectorFormula,                // Element type (16-bit BV)
                IntValue,
                ShortValue,
                short[]> {

    private static final int BIT_WIDTH = 16;

    private static final String symbolicPrefix = AbstractArrayValue.getSymbolicArrayPrefix() + ShortValue.getSymbolicPrefix();

    public IntValue size;

    /**
     * Creates a new symbolic array that contains short values
     *
     * @param context The SolverContext to create formulas in the native Solver environment.
     * @param size The size of the array
     * @param address The address of the array reference
     */
    public ShortArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.getBitvectorTypeWithSize(BIT_WIDTH), symbolicPrefix, size, address);
        concrete = new short[size.concrete];
        initArray(size.concrete);
    }

    public ShortArrayValue(
            SolverContext context,
            IntValue size,
            int address,
            IntValue parentRefIdx,
            ArrayArrayValue parentRef) {
        this(context, size, address);
        this.parentRefIdx = parentRefIdx;
        this.parentRef = parentRef;
    }

    public ShortArrayValue(SolverContext context, short[] concrete, int address) {
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

    /**
     * Returns the default value for the type of array.
     *
     * @return The default value for the type of array.
     */
    @Override
    BitvectorFormula getDefaultValue() {
        return context.getFormulaManager().getBitvectorFormulaManager().makeBitvector(BIT_WIDTH, 0);
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
    public ShortValue getElement(IntValue idx) {
        return new ShortValue(context, concrete[idx.concrete], amgr.select(formula, idx.asIntegerFormula()));
    }

    /**
     * Stores the formula representing an element at the position specified by the index formula.
     *
     * @param idx The index formula.
     * @param val The element formula.
     */
    public void storeElement(IntValue idx, ShortValue val) {
        formula = amgr.store(formula, idx.asIntegerFormula(), val.formula);
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
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        for (int i = 0; i < size; i++) {
            formula = amgr.store(formula, getIndex(i), bvmgr.makeBitvector(BIT_WIDTH, 0));
            concrete[i] = 0;
        }
    }

    /**
     * Initializes the array with specific concrete values.
     *
     * @param concreteArray The concrete values to initialize the array with.
     */
    protected void initArray(short[] concreteArray) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        for (int i = 0; i < concreteArray.length; i++) {
            formula = amgr.store(formula, getIndex(i), bvmgr.makeBitvector(BIT_WIDTH, concreteArray[i]));
        }
    }

    @Override
    public ShortArrayValue asShortArrayValue() {
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
        return genericToString("[S");
    }
}
