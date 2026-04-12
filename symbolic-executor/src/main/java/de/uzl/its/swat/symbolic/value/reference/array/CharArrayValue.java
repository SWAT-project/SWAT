package de.uzl.its.swat.symbolic.value.reference.array;

import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for Arrays that contain char values.
 * Uses BitvectorFormula (16-bit unsigned) for elements.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class CharArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,  // Index type (Int)
                BitvectorFormula,                // Element type (16-bit BV, unsigned)
                IntValue,
                CharValue,
                char[]> {

    private static final int BIT_WIDTH = 16;


    private static final String symbolicPrefix = AbstractArrayValue.getSymbolicArrayPrefix() + CharValue.getSymbolicPrefix();

    public CharArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.getBitvectorTypeWithSize(BIT_WIDTH), symbolicPrefix, size, address);
        concrete = new char[size.concrete];
        initArray(size.concrete);
    }

    public CharArrayValue(
            SolverContext context,
            IntValue size,
            int address,
            IntValue parentRefIdx,
            ArrayArrayValue parentRef) {
        this(context, size, address);
        this.parentRefIdx = parentRefIdx;
        this.parentRef = parentRef;
    }

    public CharArrayValue(SolverContext context, char[] concrete, int address) {
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
     * Returns the default value for the type of array (null character '\u0000').
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
    public CharValue getElement(IntValue idx) {
        return new CharValue(context, concrete[idx.concrete], amgr.select(formula, idx.asIntegerFormula()));
    }

    /**
     * Stores the formula representing an element at the position specified by the index formula.
     *
     * @param idx The index formula.
     * @param val The element formula.
     */
    @Override
    public void storeElement(IntValue idx, CharValue val) {
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
            concrete[i] = '\u0000';
        }
    }

    /**
     * Initializes the array with specific concrete values.
     *
     * @param concreteArray The concrete values to initialize the array with.
     */
    protected void initArray(char[] concreteArray) {
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        for (int i = 0; i < concreteArray.length; i++) {
            formula = amgr.store(formula, getIndex(i), bvmgr.makeBitvector(BIT_WIDTH, concreteArray[i]));
        }
    }

    @Override
    public CharArrayValue asCharArrayValue() {
        return this;
    }

    @Override
    public StringValue asStringValue() {
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        BitvectorFormulaManager bvmgr = context.getFormulaManager().getBitvectorFormulaManager();
        StringFormula s = smgr.makeString("");
        for (int i = 0; i < concrete.length; i++) {
            // Convert BV element to IntegerFormula for string operations
            BitvectorFormula bvElement = amgr.select(formula, getIndex(i));
            NumeralFormula.IntegerFormula intElement = bvmgr.toIntegerFormula(bvElement, false);  // unsigned
            StringFormula ch = smgr.fromCodePoint(intElement);
            s = smgr.concat(s, ch);
        }
        return new StringValue(context, new String(concrete), s, -1);
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
        return genericToString("[C");
    }
}
