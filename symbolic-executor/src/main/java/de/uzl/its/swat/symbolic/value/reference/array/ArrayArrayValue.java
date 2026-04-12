package de.uzl.its.swat.symbolic.value.reference.array;

import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.NotSupportedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.TypeException;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.DataType;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import java.util.ArrayList;
import java.util.Arrays;
import org.sosy_lab.java_smt.api.*;

public class ArrayArrayValue<TE extends Formula>
        extends AbstractArrayValue<
                NumeralFormula.IntegerFormula,
                TE,
                IntValue,
                AbstractArrayValue<?, ?, ?, ?, ?>,
                ArrayList<AbstractArrayValue>> {

    private static final String symbolicPrefix = AbstractArrayValue.getSymbolicArrayPrefix() + AbstractArrayValue.getSymbolicArrayPrefix();

    public IntValue size;

    public String desc;
    private IntValue[] dims;

    public ArrayArrayValue(
            SolverContext context,
            IntValue[] dims,
            int address,
            FormulaType elementFormulaType,
            String desc,
            IntValue parentRefIdx,
            ArrayArrayValue parentRef) throws TypeException {
        super(context, FormulaType.IntegerType, elementFormulaType, symbolicPrefix, dims[0], address);
        this.size = dims[0];

        try {
            concrete = new ArrayList<>();
        } catch (NegativeArraySizeException e) {
        }
        this.dims = dims;
        this.desc = desc;
        Value.symbol = Value.symbol + Value.inc;
        this.formula = amgr.makeArray("A_" + (Value.symbol - Value.inc), formulaType);
        this.parentRefIdx = parentRefIdx;
        this.parentRef = parentRef;
        initSymbolicArray();
    }

    private static FormulaType getArrayFormulaType(String desc) throws TypeException {

        FormulaType arrType = null;

        for (int i = desc.length() - 1; i >= 1; i--) {
            char c = desc.charAt(i);

            // Handle the object type
            if (c == ';') {
                int objectTypeStart = desc.lastIndexOf('L', i);
                String objectType = desc.substring(objectTypeStart, i + 1);

                if (DataType.getDataType(objectType) == DataType.STRING_TYPE) {
                    arrType =
                            FormulaType.getArrayType(
                                    FormulaType.IntegerType, FormulaType.StringType);
                } else {
                    throw new TypeException(c);
                }

                // Skip to the character before 'L' as we've processed the whole object type
                i = objectTypeStart;
                continue;
            }

            arrType =
                    switch (DataType.getDataType(String.valueOf(c))) {
                        case ARRAY_TYPE -> FormulaType.getArrayType(
                                FormulaType.IntegerType, arrType);
                        // Use bitvector types matching what the concrete array types use
                        case INTEGER_TYPE -> FormulaType.getArrayType(
                                FormulaType.IntegerType, FormulaType.getBitvectorTypeWithSize(32));
                        case LONG_TYPE -> FormulaType.getArrayType(
                                FormulaType.IntegerType, FormulaType.getBitvectorTypeWithSize(64));
                        case SHORT_TYPE, CHAR_TYPE -> FormulaType.getArrayType(
                                FormulaType.IntegerType, FormulaType.getBitvectorTypeWithSize(16));
                        case BYTE_TYPE -> FormulaType.getArrayType(
                                FormulaType.IntegerType, FormulaType.getBitvectorTypeWithSize(8));
                        case BOOLEAN_TYPE -> FormulaType.getArrayType(
                                FormulaType.IntegerType, FormulaType.BooleanType);
                        case DOUBLE_TYPE -> FormulaType.getArrayType(
                                FormulaType.IntegerType, DoubleValue.precision);
                        case FLOAT_TYPE -> FormulaType.getArrayType(
                                FormulaType.IntegerType, FloatValue.precision);
                        default -> throw new TypeException(c);
                    };
        }
        return arrType;
    }

    public static ArrayArrayValue arrayMagic(SolverContext context, String desc, IntValue[] dims) throws TypeException {
        FormulaType arrType = getArrayFormulaType(desc.substring(1));
        ArrayArrayValue arr =
                new ArrayArrayValue<ArrayFormula<?, ?>>(
                        context, dims, -1, arrType, desc, null, null);

        return arr;
    }

    private void init1DSymbolicArray(String newDesc, IntValue[] newDims) throws TypeException {
        DataType elementType = DataType.getDataType(newDesc.substring(1));
        AbstractArrayValue arr;

        for (int i = 0; i < size.concrete; i++) {
            switch (elementType) {
                case INTEGER_TYPE -> arr =
                        new IntArrayValue(context, newDims[0], -1, new IntValue(context, i), this);
                case BOOLEAN_TYPE -> arr =
                        new BooleanArrayValue(
                                context, newDims[0], -1, new IntValue(context, i), this);
                case BYTE_TYPE -> arr =
                        new ByteArrayValue(context, newDims[0], -1, new IntValue(context, i), this);
                case CHAR_TYPE -> arr =
                        new CharArrayValue(context, newDims[0], -1, new IntValue(context, i), this);
                case DOUBLE_TYPE -> arr =
                        new DoubleArrayValue(
                                context, newDims[0], -1, new IntValue(context, i), this);
                case FLOAT_TYPE -> arr =
                        new FloatArrayValue(
                                context, newDims[0], -1, new IntValue(context, i), this);
                case LONG_TYPE -> arr =
                        new LongArrayValue(context, newDims[0], -1, new IntValue(context, i), this);
                case SHORT_TYPE -> arr =
                        new ShortArrayValue(
                                context, newDims[0], -1, new IntValue(context, i), this);
                case STRING_TYPE -> arr =
                        new StringArrayValue(
                                context, newDims[0], -1, new IntValue(context, i), this);
                default -> throw new TypeException(elementType);
            }
            formula = amgr.store(formula, getIndex(i), (TE) arr.formula);
            concrete.add(i, arr);
        }
    }

    private void initSymbolicArray() throws TypeException {
        String newDesc = desc.substring(1);
        IntValue[] newDims = new IntValue[dims.length - 1];
        System.arraycopy(dims, 1, newDims, 0, dims.length - 1);
        if (elementIsPrimitiveArray(newDesc)) {
            init1DSymbolicArray(newDesc, newDims);

        } else {
            for (int i = 0; i < size.concrete; i++) {
                ArrayArrayValue arr =
                        new ArrayArrayValue(
                                context,
                                newDims,
                                -1,
                                getArrayFormulaType(newDesc.substring(1)),
                                newDesc,
                                new IntValue(context, i),
                                this);
                formula = amgr.store(formula, getIndex(i), (TE) arr.formula);
                concrete.add(i, arr);
            }
        }
    }

    @Override
    TE getDefaultValue() throws NotSupportedException {
        throw new NotSupportedException(
                "MultiIntArrayValue::getDefaultValue: Not supported on multi-dimensional arrays");
    }

    @Override
    NumeralFormula.IntegerFormula getIndex(int i) {
        return context.getFormulaManager().getIntegerFormulaManager().makeNumber(i);
    }

    private boolean elementIsPrimitiveArray(String desc) {
        return desc.chars().filter(ch -> ch == '[').count() == 1;
    }

    /*
    private AbstractArrayValue<?, ?, ?, ?, ?> getElementPrimitiveArray(IntValue idx){

        //DataType elementType = DataType.getDataType(desc.substring(2));
        AbstractArrayValue arr = concrete.get(idx.concrete);
        Formula elemFormula = amgr.select(formula, idx.formula);
        arr.formula = elemFormula;
        arr.parentRefIdx = idx;
        arr.parentRef = this;
        return arr;
    }

    @Override
    public AbstractArrayValue<?, ?, ?, ?, ?> getElement(IntValue idx) {
        if(elementIsPrimitiveArray(desc)){
            return getElementPrimitiveArray(idx);
        } else{
            ArrayArrayValue arr = (ArrayArrayValue) concrete.get(idx.concrete);
            arr.formula = amgr.select(formula, idx.formula);
            arr.parentRefIdx = idx;
            arr.parentRef = this;
            return arr;
        }
    }
    */

    @Override
    public AbstractArrayValue<?, ?, ?, ?, ?> getElement(IntValue idx) {
        AbstractArrayValue arr = concrete.get(idx.concrete);
        arr.formula = amgr.select(formula, idx.asIntegerFormula());
        arr.parentRefIdx = idx;
        arr.parentRef = this;
        return arr;
    }

    public void updateFormula(IntValue idx, TE val) {
        formula = amgr.store(formula, idx.asIntegerFormula(), val);
        if (parentRef != null) {
            parentRef.updateFormula(parentRefIdx, formula);
        }
    }

    @Override
    public void storeElement(IntValue idx, AbstractArrayValue val) {
        concrete.set(idx.concrete, val);
        formula = amgr.store(formula, idx.asIntegerFormula(), (TE) val.formula);
        if (parentRef != null) {
            parentRef.updateFormula(parentRefIdx, formula);
        }
    }

    @Override
    protected void initArray(int size) throws NotSupportedException {
        throw new NotSupportedException(
                "MultiIntArrayValue::initArray: Not supported on multi-dimensional arrays");
    }

    @Override
    protected String getSymbolicPrefix() {
        return symbolicPrefix;
    }

    @Override
    public String toString() {
        // concat values in array list with custom char into string
        String size = Arrays.stream(dims).map(IntValue::toString).reduce("", (a, b) -> a + b + "x");
        String formulaString = this.formula.toString();
        if (formulaString.length() > Config.instance().getLoggingFormulaLength()) {
            formulaString =
                    formulaString.substring(0, Config.instance().getLoggingFormulaLength()) + "...";
        }
        return desc + " @" + address + " (" + size + ", " + formulaString + ")";
    }
}
