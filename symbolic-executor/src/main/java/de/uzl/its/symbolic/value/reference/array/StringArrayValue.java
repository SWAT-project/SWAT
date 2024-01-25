package de.uzl.its.symbolic.value.reference.array;

import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.lang.StringValue;
import java.util.HashMap;
import org.sosy_lab.java_smt.api.*;

/**
 * Wrapper for Arrays that contain String values.
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class StringArrayValue
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula, StringFormula, IntValue, StringValue, String[]> {

    public HashMap<Integer, Integer> addressMap = new HashMap<Integer, Integer>();

    public StringArrayValue(SolverContext context, IntValue size, int address) {
        super(context, FormulaType.IntegerType, FormulaType.StringType, size, address);
        concrete = new String[size.concrete];
        initArray(size.concrete);
    }

    public StringArrayValue(
            SolverContext context,
            IntValue size,
            int address,
            IntValue parentRefIdx,
            ArrayArrayValue parentRef) {
        this(context, size, address);
        this.parentRefIdx = parentRefIdx;
        this.parentRef = parentRef;
    }

    public StringArrayValue(SolverContext context, String[] concrete, int address) {
        super(
                context,
                FormulaType.IntegerType,
                FormulaType.StringType,
                new IntValue(context, concrete.length),
                address);
        this.concrete = concrete;
        initArray(concrete);
    }

    @Override
    StringFormula getDefaultValue() {
        return context.getFormulaManager().getStringFormulaManager().makeString("");
    }

    @Override
    NumeralFormula.IntegerFormula getIndex(int i) {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(i);
    }

    @Override
    public StringValue getElement(IntValue idx) {
        // ToDo Can we get the actual address?
        return new StringValue(
                context,
                concrete[idx.concrete],
                amgr.select(formula, idx.formula),
                addressMap.get(idx.concrete));
    }

    public void storeElement(IntValue idx, StringValue val) {
        try {
            concrete[idx.concrete] = val.concrete;
            formula = amgr.store(formula, idx.formula, val.formula);
            addressMap.put(idx.concrete, val.getAddress());

            if (parentRef != null) {
                parentRef.updateFormula(parentRefIdx, formula);
            }

        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    @Override
    protected void initArray(int size) {
        // ToDo (Nils): Is this needed or correct?
        // For Multinewarrays it seems needed as rthe values are else free i.e. arbitrary
        for (int i = 0; i < size; i++) {
            formula = amgr.store(formula, getIndex(i), getDefaultValue());
            concrete[i] = "";
            addressMap.put(i, 0);
        }
    }

    protected void initArray(String[] array) {
        StringFormulaManager smgr = context.getFormulaManager().getStringFormulaManager();
        // ToDo (Nils): Is this needed or correct?
        for (int i = 0; i < array.length; i++) {
            formula = amgr.store(formula, getIndex(i), smgr.makeString(concrete[i]));
        }
    }

    @Override
    public AbstractArrayValue<
                    NumeralFormula.IntegerFormula, StringFormula, IntValue, StringValue, String[]>
            asArrayValue() {
        return this;
    }

    @Override
    public String toString() {
        return "StringArrayValue @"
                + Integer.toHexString(address)
                + " { size="
                + size
                + ", formula="
                + formula
                + "}";
    }
}
