package de.uzl.its.symbolic.value.reference.array;

import de.uzl.its.symbolic.value.Value;
import de.uzl.its.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.symbolic.value.reference.ObjectValue;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Wrapper for arrays of references (non-primitive types)
 *
 * @author Nils Loose
 * @version 2022.07.25
 */
public class ObjectArrayValue extends ObjectValue {

    /** Size of the array */
    public final IntValue size;

    /**
     * Creates an Array of references To create multidimensional arrays, this class is nested
     *
     * @param className The type of the reference
     * @param size The size of the array (this dimension)
     */
    public ObjectArrayValue(SolverContext context, String className, IntValue size) {
        super(context, className, size);
        this.size = size;
    }

    public BooleanFormula checkIndex(IntValue idx) {
        BooleanFormulaManager bmgr = context.getFormulaManager().getBooleanFormulaManager();
        IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
        return bmgr.and(
                imgr.lessThan(idx.formula, size.formula),
                imgr.greaterOrEquals(idx.formula, imgr.makeNumber(0)));
    }

    /**
     * Retrieves a reference from an Array ToDo (Nils): The index and value are currently not
     * tracked symbolically See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/59">Issue
     * 59</a>
     *
     * @param idx The index of the position to retrieve
     * @return The reference value at position idx
     */
    public Value<?, ?> getElement(IntValue idx) {
        return getField(idx.concrete);
    }

    /**
     * Stores a reference value in an array at index idx ToDo (Nils): The index and value are
     * currently not tracked symbolically See: <a
     * href="https://git.its.uni-luebeck.de/research-projects/pet-hmr/knife-fuzzer/-/issues/59">Issue
     * 59</a>
     *
     * @param idx The index where to store the Value
     * @param ref The value to be stored
     */
    public void storeElement(IntValue idx, Value<?, ?> ref) {
        setField(idx.concrete, ref);
    }

    /**
     * Cast replacement
     *
     * @return ObjectArrayValue this
     */
    @Override
    public ObjectArrayValue asObjectArrayValue() {
        return this;
    }

    /**
     * Getter for the concrete size of the array
     *
     * @return The concrete size of the array
     */
    public IntValue getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "[" + className + " @" + Integer.toHexString(address) + " (" + size + " x 1" + ")";
    }
}
