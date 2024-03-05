package de.uzl.its.symbolic.value;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.uzl.its.swat.config.Config;
import de.uzl.its.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.symbolic.value.primitive.numeric.integral.*;
import de.uzl.its.symbolic.value.reference.ObjectValue;
import de.uzl.its.symbolic.value.reference.lang.StringValue;
import lombok.Getter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverContext;

/** Abstract value of any type. Base class for the concrete values. */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class Value<T, K> {

    // ToDo: Turn this into a private var with a getter that auto increments
    protected static int symbol = 0; // Number of the next symbol
    protected static final int inc = 1;

    public T formula;
    public K concrete;
    public SolverContext context;

    @Getter
    protected String name;

    public static void reset() {
        symbol = 0;
    }

    public String getType() {
        String[] type = concrete.getClass().getName().split("\\.");
        return type[type.length - 1];
    }

    public String MAKE_SYMBOLIC(String namePrefix) {
        throw new RuntimeException("Cannot make " + this + " symbolic");
        // return "";
    }

    public String MAKE_SYMBOLIC() {
        throw new RuntimeException("Cannot make " + this + " symbolic");
        // return "";
    }

    public String MAKE_SYMBOLIC(long idx) {
        throw new RuntimeException("Cannot make " + this + " symbolic");
        // return "";
    }

    protected void initSymbolic(String namePrefix) {
        assert namePrefix != null;
        name = namePrefix + "_" + symbol;
        symbol = symbol + inc;
    }

    protected void initSymbolic(String namePrefix, long idx) {
        name = namePrefix + "_" + idx;
    }

    public BooleanFormula getBounds(boolean upper) {
        throw new RuntimeException(
                "Bounds not implemented for: " + this.getClass().getSimpleName());
    }

    public Object getConcrete() {
        return concrete;
    }

    public String getConcreteEncoded() {
        throw new RuntimeException(
                "'getConcreteEncoded' is not implemented yet for this type: " + this);
    }

    public ByteValue asByteValue() {
        throw new RuntimeException("'asByteValue' is not implemented yet for this type: " + this);
    }

    public IntValue asIntValue() {
        throw new RuntimeException("'asIntValue' is not implemented yet for this type: " + this);
    }

    public ShortValue asShortValue() {
        throw new RuntimeException("'asShortValue' is not implemented yet for this type: " + this);
    }

    public DoubleValue asDoubleValue() {
        throw new RuntimeException("'asDoubleValue' is not implemented yet for this type: " + this);
    }

    public BooleanValue asBooleanValue() {
        throw new RuntimeException(
                "'asBooleanValue' is not implemented yet for this type: " + this);
    }

    public CharValue asCharValue() {
        throw new RuntimeException("'asCharValue' is not implemented yet for this type: " + this);
    }

    public ObjectValue asObjectValue() {
        throw new RuntimeException("'asObjectValue' is not implemented yet for this type: " + this);
    }

    public FloatValue asFloatValue() {
        throw new RuntimeException("'asFloatValue' is not implemented yet for this type: " + this);
    }

    public LongValue asLongValue() {
        throw new RuntimeException("'asLongValue' is not implemented yet for this type: " + this);
    }

    public StringValue asStringValue() {
        throw new RuntimeException("'asStringValue' is not implemented yet for this type: " + this);
    }

    public NumericalValue<?, ?> asNumericalValue() {
        throw new RuntimeException("'asStringValue' is not implemented yet for this type: " + this);
    }

    /**
     * Returns the string representation of the value. Should be implemented by each inheriting class.
     * @return the string representation of the value
     */
    @Override
    public String toString() {
        throw new RuntimeException("'toString' is not implemented yet for this type: " + this);
    }


}

