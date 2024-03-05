package de.uzl.its.symbolic.value.reference.array;

import de.uzl.its.swat.config.Config;
import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.ObjectValue;
import javax.annotation.Nullable;
import org.sosy_lab.java_smt.api.*;

/**
 * Abstract class for symbolic arrays.
 *
 * @author Nils Loose
 * @version 2022.07.15
 */
public abstract class AbstractArrayValue<
                TI extends Formula,
                TE extends Formula,
                VI extends Value<?, ?>,
                VE extends Value<?, ?>,
                K>
        extends ObjectValue<ArrayFormula<TI, TE>, K> {

    /** Java-smt formula manager for array theory */
    protected ArrayFormulaManager amgr;

    /** Solver context to interact with the native solver (used for creating formulas) */
    protected SolverContext context;

    /** Formula type used for the indices of the symbolic array */
    protected FormulaType<TI> indexFormulaType;

    /** Formula type used for the elements of the symbolic array */
    protected FormulaType<TE> elementFormulaType;

    /** Formula type representing the index - element pair */
    protected FormulaType.ArrayFormulaType<TI, TE> formulaType;

    /** Integer wrapper that represents the size of the array */
    public IntValue size;

    @Nullable protected IntValue parentRefIdx;
    @Nullable protected ArrayArrayValue parentRef;

    public AbstractArrayValue(
            SolverContext context,
            FormulaType<TI> indexFormulaType,
            FormulaType<TE> elementFormulaType,
            IntValue size,
            int address) {
        super(context, 100, address);
        this.context = context;
        this.amgr = context.getFormulaManager().getArrayFormulaManager();
        Value.symbol = Value.symbol + Value.inc;
        this.indexFormulaType = indexFormulaType;
        this.elementFormulaType = elementFormulaType;
        formulaType = FormulaType.getArrayType(indexFormulaType, elementFormulaType);

        this.formula = amgr.makeArray("a" + (Value.symbol - Value.inc), formulaType);
        this.size = size;
        this.parentRefIdx = null;
        this.parentRef = null;
    }

    public AbstractArrayValue(
            SolverContext context,
            FormulaType<TI> indexFormulaType,
            FormulaType<TE> elementFormulaType,
            ArrayFormula<TI, TE> formula,
            IntValue size,
            int address) {
        super(context, 100, address);
        this.context = context;
        this.amgr = context.getFormulaManager().getArrayFormulaManager();
        this.indexFormulaType = indexFormulaType;
        this.elementFormulaType = elementFormulaType;
        formulaType = FormulaType.getArrayType(indexFormulaType, elementFormulaType);
        this.formula = formula;
        this.size = size;
    }

    /**
     * Returns the default value for the type of array.
     *
     * @return The default value for the type of array.
     */
    abstract TE getDefaultValue();

    /**
     * Returns a formula representing the index specified by i.
     *
     * @param i The integer representation (concrete value) of the index.
     * @return The formula representing the index.
     */
    abstract TI getIndex(int i);

    /**
     * Returns the formula that represents the element at the specified index.
     *
     * @param idx The formula that specifies the index.
     * @return The formula that specifies the retrieved/ retrievable element.
     */
    public abstract VE getElement(IntValue idx);

    /**
     * Stores the formula representing an element at the position specified by the index formula.
     *
     * @param idx The index formula.
     * @param val The element formula.
     */
    public abstract void storeElement(IntValue idx, VE val);

    public BooleanFormula checkIndex(IntValue idx) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        return bmgr.and(
                imgr.lessThan(idx.formula, size.formula),
                imgr.greaterOrEquals(idx.formula, imgr.makeNumber(0)));
    }

    /**
     * Initializes the array with the default value.
     *
     * @param size The size of the array.
     */
    protected abstract void initArray(int size);

    @Override
    public AbstractArrayValue<TI, TE, VI, VE, K> asArrayValue() {
        return this;
    }

    public IntArrayValue asIntArrayValue() {
        throw new RuntimeException("Cannot convert AbstractArrayValue to IntArrayValue");
    }

    public BooleanArrayValue asBooleanArrayValue() {
        throw new RuntimeException("Cannot convert AbstractArrayValue to BooleanArrayValue");
    }

    public LongArrayValue asLongArrayValue() {
        throw new RuntimeException("Cannot convert AbstractArrayValue to LongArrayValue");
    }

    public FloatArrayValue asFloatArrayValue() {
        throw new RuntimeException("Cannot convert AbstractArrayValue to FloatArrayValue");
    }

    public ObjectArrayValue asObjectArrayValue() {
        throw new RuntimeException("Cannot convert AbstractArrayValue to ObjectArrayValue");
    }

    public ByteArrayValue asByteArrayValue() {
        throw new RuntimeException("Cannot convert AbstractArrayValue to ByteArrayValue");
    }

    public CharArrayValue asCharArrayValue() {
        throw new RuntimeException("Cannot convert AbstractArrayValue to CharArrayValue");
    }

    public DoubleArrayValue asDoubleArrayValue() {
        throw new RuntimeException("Cannot convert AbstractArrayValue to DoubleArrayValue");
    }

    public ShortArrayValue asShortArrayValue() {
        throw new RuntimeException("Cannot convert AbstractArrayValue to DoubleArrayValue");
    }

    public SolverContext getContext() {
        return context;
    }

    public String genericToString(String type) {
        String formulaString = this.formula.toString();
        if (formulaString.length() > Config.instance().getFormulaPrintLength()) {
            formulaString = formulaString.substring(0, Config.instance().getFormulaPrintLength()) + "...";
        }
        return type + " @"
                + Integer.toHexString(address)
                + " ("
                + size
                + " x 1, "
                + formulaString
                + ")";
    }
}
