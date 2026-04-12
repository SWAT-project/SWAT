package de.uzl.its.swat.symbolic.value;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.SWATAssert;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.symbolic.value.primitive.numeric.NumericalValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.*;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.*;
import lombok.Getter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Set;

/** Abstract value of any type. Base class for the concrete values. */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class Value<T, K> {

    // ToDo: Turn this into a private var with a getter that auto increments
    protected static int symbol = 0; // Number of the next symbol
    protected static final int inc = 1;

    public T formula;
    public K concrete;
    public SolverContext context;
    @Getter protected String name;
    protected static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();
    protected static long nextInternalID = 0;
    protected long internalID = -1;

    public Value() {
        this.internalID = nextInternalID++;
    }
    
    public static void reset() {
        symbol = 0;
    }

    public String getType() {
        String[] type = concrete.getClass().getName().split("\\.");
        return type[type.length - 1];
    }

    public String getSymPrefix() throws NotImplementedException {
        throw new NotImplementedException("getSymPrefix()", this.getClass());
    }
    public String MAKE_SYMBOLIC(String prefixOrIdx) throws NotImplementedException {
        throw new NotImplementedException("MAKE_SYMBOLIC(String prefixOrIdx)", this.getClass());
    }

    public String MAKE_SYMBOLIC() throws NotImplementedException {
        throw new NotImplementedException("MAKE_SYMBOLIC()", this.getClass());
    }

    public String MAKE_SYMBOLIC(long idx) throws NotImplementedException {
        throw new NotImplementedException("MAKE_SYMBOLIC(long idx)", this.getClass());
    }


    // Todo: Is that method actually used still? the idx should (always?) be the uid from Intrinsics?
    protected void initSymbolic(String namePrefix) {
        SWATAssert.enforce(namePrefix != null, "Name prefix cannot be null");
        name = namePrefix + "_" + symbol;
        symbol = symbol + inc;
    }
    //Todo: this is a hotfix...
    protected void initSymbolicWithoutIdx(String symbolicName) {
        SWATAssert.enforce(symbolicName != null, "Name prefix cannot be null");
        name = symbolicName;
    }

    protected void initSymbolic(String namePrefix, long idx) {
        name = namePrefix + "_" + idx;
    }

    protected void initSymbolic(String namePrefix, String idx) {
        name = namePrefix + "_" + idx;
    }


    public boolean isSymbolic() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public boolean isSymbolic(Set<Value<?, ?>> visited) throws NotImplementedException {
        // Should be overridden by subclasses that can cause circular checks (e.g. ObjectValue)
        return isSymbolic();
    }

    public ImmutableSet<String> getSymbolicVariables() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public BooleanFormula getBounds(boolean upper) throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public Object getConcrete() {
        return concrete;
    }

    public String getConcreteEncoded() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public ByteValue asByteValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public IntValue asIntValue() throws NotImplementedException, ValueConversionException {
        throw new NotImplementedException(this.getClass());
    }

    public ShortValue asShortValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public DoubleValue asDoubleValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public BooleanValue asBooleanValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public CharValue asCharValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public ObjectValue asObjectValue() throws NotImplementedException, ValueConversionException {
        throw new NotImplementedException(this.getClass());
    }

    public FloatValue asFloatValue() throws NotImplementedException, ValueConversionException {
        throw new NotImplementedException(this.getClass());
    }

    public LongValue asLongValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public StringValue asStringValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public IntegerObjectValue asIntegerObjectValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public BooleanObjectValue asBooleanObjectValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public ByteObjectValue asByteObjectValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public LongObjectValue asLongObjectValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public CharacterObjectValue asCharacterObjectValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public ShortObjectValue asShortObjectValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public DoubleObjectValue asDoubleObjectValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public FloatObjectValue asFloatObjectValue() throws NotImplementedException {
        throw new NotImplementedException(this.getClass());
    }

    public NumericalValue<?, ?> asNumericalValue() throws NotImplementedException{
        throw new NotImplementedException(this.getClass());
    }

    /**
     * Returns the string representation of the value. Should be implemented by each inheriting
     * class.
     *
     * @return the string representation of the value
     */
    @Override
    public String toString() {
        SWATAssert.enforce(false, "toString() not implemented for " + this.getClass());
        return "Abstract Value class!";
    }
}
