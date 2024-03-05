package de.uzl.its.symbolic.value.reference.array;

import de.uzl.its.symbolic.value.primitive.numeric.integral.ByteValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for Arrays that contain byte values.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class ByteArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,
                NumeralFormula.IntegerFormula,
                IntValue,
                ByteValue,
                byte[]> {

    public ByteArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.IntegerType, size, address);
        concrete = new byte[size.concrete];
        initArray(size.concrete);
    }

    public ByteArrayValue(
            SolverContext context,
            IntValue size,
            int address,
            IntValue parentRefIdx,
            ArrayArrayValue parentRef) {
        this(context, size, address);
        this.parentRefIdx = parentRefIdx;
        this.parentRef = parentRef;
    }

    public ByteArrayValue(SolverContext context, byte[] concrete, int address) {
        super(
                context,
                FormulaType.IntegerType,
                FormulaType.IntegerType,
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
    NumeralFormula.IntegerFormula getDefaultValue() {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(0);
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
    public ByteValue getElement(IntValue idx) {
        return new ByteValue(context, concrete[idx.concrete], amgr.select(formula, idx.formula));
    }

    /**
     * Stores the formula representing an element at the position specified by the index formula.
     *
     * @param idx The index formula.
     * @param val The element formula.
     */
    public void storeElement(IntValue idx, ByteValue val) {
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
            concrete[i] = 0;
        }
    }

    protected void initArray(byte[] array) {
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        // ToDo (Nils): Is this needed or correct?
        for (int i = 0; i < array.length; i++) {
            formula = amgr.store(formula, getIndex(i), imgr.makeNumber(concrete[i]));
        }
    }

    @Override
    public ByteArrayValue asByteArrayValue() {
        return this;
    }


    /**
     * Returns the string representation of the value used to visualize the stack. The representation is not complete.
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("[B");
    }
}
