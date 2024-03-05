package de.uzl.its.symbolic.value.reference.array;

import de.uzl.its.symbolic.value.primitive.numeric.integral.CharValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.lang.StringValue;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for Arrays that contain char values.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class CharArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,
                NumeralFormula.IntegerFormula,
                IntValue,
                CharValue,
                char[]> {

    public CharArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.IntegerType, size, address);
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
                FormulaType.IntegerType,
                new IntValue(context, concrete.length),
                address);
        this.concrete = concrete;
        initArray(concrete);
    }

    /**
     * Returns the default value for the type of array. ToDo (Nils): Is the null value correctly
     * initialized?
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
    public CharValue getElement(IntValue idx) {
        return new CharValue(context, concrete[idx.concrete], amgr.select(formula, idx.formula));
    }

    /**
     * Stores the formula representing an element at the position specified by the index formula.
     *
     * @param idx The index formula.
     * @param val The element formula.
     */
    @Override
    public void storeElement(IntValue idx, CharValue val) {
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
            concrete[i] = '\u0000';
        }
    }

    protected void initArray(char[] array) {
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        // ToDo (Nils): Is this needed or correct?
        for (int i = 0; i < array.length; i++) {
            formula = amgr.store(formula, getIndex(i), imgr.makeNumber(concrete[i]));
        }
    }

    @Override
    public CharArrayValue asCharArrayValue() {
        return this;
    }

    @Override
    public StringValue asStringValue() {
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        StringFormula s = smgr.makeString("");
        for (int i = 0; i < concrete.length; i++) {
            StringFormula ch =
                    smgr.charAt(
                            smgr.makeString(CharValue.getASCIIchars(0, 591)),
                            amgr.select(formula, getIndex(i)));
            s = smgr.concat(s, ch);
        }
        return new StringValue(context, new String(concrete), s, -1);
    }


    /**
     * Returns the string representation of the value used to visualize the stack. The representation is not complete.
     * @return the string representation of the value.
     */
    @Override
    public String toString() {
        return genericToString("[C");
    }
}
