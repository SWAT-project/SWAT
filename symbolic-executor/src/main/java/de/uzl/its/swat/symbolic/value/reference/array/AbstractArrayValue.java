package de.uzl.its.swat.symbolic.value.reference.array;

import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.NotSupportedException;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import javax.annotation.Nullable;

import lombok.Getter;
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
    @Getter
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
    @Getter
    private static final String symbolicArrayPrefix = "[";

    public AbstractArrayValue(
            SolverContext context,
            FormulaType<TI> indexFormulaType,
            FormulaType<TE> elementFormulaType,
            String symbolicPrefix,
            IntValue size,
            int address) {
        super(context, address);
        this.context = context;
        this.amgr = context.getFormulaManager().getArrayFormulaManager();
        Value.symbol = Value.symbol + Value.inc;
        this.indexFormulaType = indexFormulaType;
        this.elementFormulaType = elementFormulaType;
        formulaType = FormulaType.getArrayType(indexFormulaType, elementFormulaType);

        this.formula = amgr.makeArray(symbolicPrefix + "_" + (Value.symbol - Value.inc), formulaType);
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
        super(context, address);
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
    abstract TE getDefaultValue() throws NotSupportedException;

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
        // Use asIntegerFormula() to convert BV index to Int for array bounds checking
        return bmgr.and(
                imgr.lessThan(idx.asIntegerFormula(), size.asIntegerFormula()),
                imgr.greaterOrEquals(idx.asIntegerFormula(), imgr.makeNumber(0)));
    }

    /**
     * Initializes the array with the default value.
     *
     * @param size The size of the array.
     */
    protected abstract void initArray(int size) throws NotSupportedException;

    @Override
    public AbstractArrayValue<TI, TE, VI, VE, K> asArrayValue() {
        return this;
    }

    public IntArrayValue asIntArrayValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public BooleanArrayValue asBooleanArrayValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public LongArrayValue asLongArrayValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public FloatArrayValue asFloatArrayValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public ObjectArrayValue asObjectArrayValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public ByteArrayValue asByteArrayValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public CharArrayValue asCharArrayValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public DoubleArrayValue asDoubleArrayValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public ShortArrayValue asShortArrayValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    /**
     * Turns this ArrayValue into a symbolic variable
     *
     * @param prefixOrIdx The prefix or index for the symbolic variable
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(String prefixOrIdx) {
        if (prefixOrIdx.matches("-?\\d+")){
            // We assume a constructed idx was passed as it is a number
            initSymbolic(getSymbolicPrefix(), prefixOrIdx);
        } else if (prefixOrIdx.matches(".*-?\\d+")){
            // Its a list which already has prefix and idx
            initSymbolicWithoutIdx(prefixOrIdx);
        } else {
            // If it's not a number we assume prefix
            initSymbolic(prefixOrIdx);
        }
        formula = amgr.makeArray(name, formulaType);
        return name;
    }

    /**
     * Turns this ArrayValue into a symbolic variable
     *
     * @param idx The index for the symbolic variable
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC(long idx) {
        initSymbolic(getSymbolicPrefix(), idx);
        formula = amgr.makeArray(name, formulaType);
        return name;
    }

    /**
     * Turns this ArrayValue into a symbolic variable
     *
     * @return The numerical identifier of this symbolic variable
     */
    @Override
    public String MAKE_SYMBOLIC() {
        initSymbolic(getSymbolicPrefix());
        formula = amgr.makeArray(name, formulaType);
        return name;
    }

    /**
     * Returns the symbolic prefix for this array type.
     * Subclasses should override this to provide their specific prefix.
     *
     * @return The symbolic prefix
     */
    protected abstract String getSymbolicPrefix();

    /**
     * Public accessor for the symbolic prefix, required by liftValue operations.
     *
     * @return The symbolic prefix
     */
    @Override
    public String getSymPrefix() {
        return getSymbolicPrefix();
    }

    /**
     * Returns bounds constraints for arrays. Arrays themselves don't have numeric bounds,
     * so this returns a tautology (always true). Individual elements within the array
     * will have their own bounds.
     *
     * @param upper Whether to return upper or lower bound (unused for arrays)
     * @return A BooleanFormula that is always true
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        return context.getFormulaManager().getBooleanFormulaManager().makeTrue();
    }

    @Override
    public boolean isSymbolic() {
        return !context.getFormulaManager().extractVariables(formula).isEmpty();
    }

    @Override
    public ImmutableSet<String> getSymbolicVariables() {
        return context.getFormulaManager().extractVariables(formula).keySet();
    }

    public String genericToString(String type) {
        String formulaString = this.formula.toString();
        if (formulaString.length() > Config.instance().getLoggingFormulaLength()) {
            formulaString =
                    formulaString.substring(0, Config.instance().getLoggingFormulaLength()) + "...";
        }
        return type
                + " @"
                + Integer.toHexString(address)
                + " ("
                + size
                + " x 1, "
                + formulaString
                + ")";
    }
}
