package de.uzl.its.swat.symbolic.value.reference;

import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.NoThreadContextException;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue;
import de.uzl.its.swat.symbolic.value.reference.array.AbstractArrayValue;
import de.uzl.its.swat.symbolic.value.reference.array.ObjectArrayValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import de.uzl.its.swat.thread.ThreadHandler;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.Thread.currentThread;

public class ObjectValue<T, K> extends Value<T, K> {

    public static final int ADDRESS_UNKNOWN = -1;
    public static final int ADDRESS_NULL = 0;
    public String className;
    @Setter @Getter protected int address; // address 0 is null, address -1 is uninitialized address

    // For array object, fields are the content of the array.
    // For plain object, fields are the instance variables.
    @Setter @Getter private Value<?, ?>[] fields;
    @Getter private IntValue nFields;

    private BooleanFormulaManager bfm;

    // Initialize
    // TODO: why is the first arg there?
    public ObjectValue(SolverContext context, int address) {
        fields = null;
        this.address = address;
        this.context = context;
        this.bfm = context.getFormulaManager().getBooleanFormulaManager();
    }

    public ObjectValue(SolverContext context, String className, IntValue nFields) {
        fields = new Value[nFields.concrete];
        this.context = context;
        this.nFields = nFields;
        address = -1;
        this.className = className;
        this.bfm = context.getFormulaManager().getBooleanFormulaManager();
    }

    public ObjectValue(SolverContext context, String className, IntValue nFields, int address) {
        fields = new Value[nFields.concrete];
        this.context = context;
        this.nFields = nFields;
        this.address = address;
        this.className = className;
        this.bfm = context.getFormulaManager().getBooleanFormulaManager();
    }

    /**
     * Returns the symbolic prefix for Object values.
     *
     * @return The symbolic prefix "O"
     */
    @Override
    public String getSymPrefix() {
        return "O";
    }

    /**
     * Returns bounds constraints for objects. Objects don't have numeric bounds,
     * so this returns a tautology (always true).
     *
     * @param upper Whether to return upper or lower bound (unused for objects)
     * @return A BooleanFormula that is always true
     */
    @Override
    public BooleanFormula getBounds(boolean upper) {
        return bfm.makeTrue();
    }

    /**
     * Generates a unique symbolic identifier for the object based on the provided prefix.
     *
     * @param namePrefix The prefix that will be used to generate the symbolic identifier
     * @return The generated symbolic identifier.
     */
    @Override
    public String MAKE_SYMBOLIC(String namePrefix) {
        MAKE_SYMBOLIC();
        if (namePrefix != null) {
            name = namePrefix + "_" + name;
        }
        return name;
    }

    /**
     * Generates a unique symbolic identifier for the object based on the datatype of the object and
     * a counter.
     *
     * @return The generated symbolic identifier.
     */
    @Override
    public String MAKE_SYMBOLIC() {
        if (name == null) {
            name = "o" + symbol;
            symbol = symbol + inc;
        }
        return name;
    }

    //Todo: should this be an empty impl? what is the benefit here? Should this actually not be here at all as we have equals methods?
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof ObjectValue<?, ?> other) {
            return (address == other.address && fields.length == other.fields.length);
        } else {
            return false;
        }
    }

    @Override
    public Object getConcrete() { // (TODO): confusing
        return address;
    } // TODO: WTF is that supposed to mean?!

    /** Private default constructor for serialization */
    private ObjectValue() {}

    /**
     * Compares if two references are identical
     *
     * @param o2 The other object
     * @return A BooleanValue representing the result
     */
    public BooleanFormula IF_ACMPEQ(ObjectValue<?, ?> o2) {
        return bfm.makeBoolean(this == o2);
    }

    /**
     * Compares if two references are not identical
     *
     * @param o2 The other object
     * @return A BooleanFormula representing the result
     */
    public BooleanFormula IF_ACMPNE(ObjectValue<?, ?> o2) {
        return bfm.makeBoolean(this != o2);
    }

    /**
     * Compares if this reference is the null value; NULL is represented by the address 0
     *
     * @return A BooleanFormula representing the result
     */
    public BooleanFormula IFNULL() {
        return bfm.makeBoolean(this.address == 0);
    }

    /**
     * Compares if this reference is not the null value NULL is represented by the address 0
     *
     * @return A BooleanFormula representing the result
     */
    public BooleanFormula IFNONNULL() {
        return bfm.makeBoolean(this.address != 0);
    }


    public Value<?, ?> getField(int fieldId) throws NoThreadContextException {
        SWATAssert.check(address != 0, "Trying to get a field of NULL (NullPointerException)");
        if (fields == null) {
            logger.warn("[{}]: Fields are null for object {}",
                    ThreadHandler.getCurrentInstruction(currentThread().getId()), this);
            return PlaceHolder.instance;
        } else {
            Value<?, ?> v = fields[fieldId];
            if (v == null) {
                logger.warn("{}]: No value for field {} in object {}",
                        ThreadHandler.getCurrentInstruction(currentThread().getId()), fieldId, this);
                return PlaceHolder.instance;
            }
            return v;
        }
    }

    /**
     * Sets the value of a field in the object
     * The object cannot be NULL (address == 0)
     * The fields array must be initialized and the fieldId must be within bounds.
     * ToDo: Be more lenient and initialize fields array if it is null
     *
     * @param fieldId The index of the field
     * @param value The value to set
     */
    public void setField(int fieldId, Value<?, ?> value) {
        // Todo: Some of the enforcement could be relaxed to checks
        SWATAssert.enforce(address != 0, "Trying to set a field of NULL");
        SWATAssert.enforce(fields != null, "Cannot set field of object without fields");
        SWATAssert.enforce(fieldId < fields.length, "Field index out of bounds");
        fields[fieldId] = value;
    }

    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args) throws NotImplementedException, ValueConversionException {
        if (this.name != null) {
            for (String prefix : PlaceHolder.valueOriginPrefixMap.values()) {
                if (!prefix.isEmpty() && this.name.startsWith(prefix)) {
                    return new PlaceHolder(true, PlaceHolder.prefixValueOriginMap.get(prefix));
                }
            }
            return PlaceHolder.symbolicInstance;
        }
        return PlaceHolder.instance;
    }

    @Override
    public ObjectValue<?, ?> asObjectValue() {
        return this;
    }

    @Override
    public FloatValue asFloatValue() throws ValueConversionException {
        throw new ValueConversionException("Cannot convert object to float");
        // return new FloatValue(this.address); // TODO not verified yet -> This should be FloatObjectValue?
    }

    public AbstractArrayValue<?, ?, ?, ?, ?> asArrayValue() throws ValueConversionException {
        throw new ValueConversionException("Cannot convert object to array");
    }

    public ObjectArrayValue asObjectArrayValue() throws NotImplementedException, ValueConversionException {
        throw new ValueConversionException("Cannot convert object to (object) array");
    }

    @Override
    public IntValue asIntValue() throws ValueConversionException  {
        throw new ValueConversionException("Cannot convert" + this.getClass().getSimpleName() +  " to IntValue");
    }

    public String toString() {
        return "ObjectValue[" + this.internalID +"] @"
                + Integer.toHexString(address)
                + " { nFields="
                + nFields
                + ", className="
                + className
                + "}";
    }


    @Override
    public boolean isSymbolic() throws NotImplementedException {
        return isSymbolic(new HashSet<>());
        // return fields != null && Arrays.stream(fields).filter(Objects::nonNull).anyMatch(Value::isSymbolic);
    }

    @Override
    public boolean isSymbolic(Set<Value<?, ?>> visited) throws NotImplementedException, RuntimeException {
        // If this object is already being checked, return false to avoid recursion
        if (!visited.add(this)) {
            return false;
        }
        return fields != null && Arrays.stream(fields)
                .filter(Objects::nonNull)
                .anyMatch(field -> {
                    try {
                        return field.isSymbolic(visited);
                    } catch (NotImplementedException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public ImmutableSet<String> getSymbolicVariables() throws NotImplementedException, RuntimeException {
        if (fields == null) {
            return ImmutableSet.of();
        }
        return Arrays.stream(fields).filter(Objects::nonNull)
                .filter(field -> {
                    try {
                        return !field.getSymbolicVariables().isEmpty();
                    } catch (NotImplementedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(field -> {
                    try {
                        return field.getSymbolicVariables().stream();
                    } catch (NotImplementedException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(ImmutableSet.toImmutableSet());
    }

    public boolean isNullValue() {
        return address == ObjectValue.ADDRESS_NULL;
    }

    @Override
    public StringValue asStringValue() throws NotImplementedException {
        if (this.address == ObjectValue.ADDRESS_NULL) {
            return new StringValue(context, "null", ObjectValue.ADDRESS_UNKNOWN);
        } else {
            throw new NotImplementedException("Cannot convert " + this.getClass().getSimpleName() + " to StringValue");
        }
    }
}
