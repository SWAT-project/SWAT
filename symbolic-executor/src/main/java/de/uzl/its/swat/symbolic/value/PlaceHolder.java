package de.uzl.its.swat.symbolic.value;

import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.instruction.Instruction;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import java.util.Map;

/** Author: Koushik Sen (ksen@cs.berkeley.edu) Date: 6/17/12 Time: 6:05 PM */
public class PlaceHolder extends Value {

    public static final Map<ValueOrigin, String> valueOriginPrefixMap;

    static {
        valueOriginPrefixMap =
                Map.ofEntries(
                        Map.entry(ValueOrigin.UNSPECIFIED, ""),
                        Map.entry(ValueOrigin.DATABASE, "db"));
    }

    public static final Map<String, ValueOrigin> prefixValueOriginMap;

    static {
        prefixValueOriginMap =
                Map.ofEntries(
                        Map.entry("", ValueOrigin.UNSPECIFIED),
                        Map.entry("db", ValueOrigin.DATABASE));
    }

    public enum ValueOrigin {
        UNSPECIFIED,
        DATABASE,
        GETFIELD,
        GETSTATIC
    }

    public final boolean isSymbolic;
    public final ValueOrigin origin;
    public final Instruction inst;
    public final ObjectValue<?, ?> referenceValue;
    public static final PlaceHolder instance = new PlaceHolder(false);
    public static final PlaceHolder symbolicInstance = new PlaceHolder(true);

    public PlaceHolder(boolean isSymbolic) {
        this.isSymbolic = isSymbolic;
        this.origin = ValueOrigin.UNSPECIFIED;
        this.inst = null;
        this.referenceValue = null;
    }

    public PlaceHolder(ValueOrigin origin, Instruction inst, ObjectValue<?, ?> referenceValue) {
        this.origin = origin;
        this.isSymbolic = false;
        this.inst = inst;
        this.referenceValue = referenceValue;
    }

    public PlaceHolder(boolean isSymbolic, ValueOrigin origin) {
        this.isSymbolic = isSymbolic;
        this.origin = origin;
        this.inst = null;
        this.referenceValue = null;
    }

    public ObjectValue<?, ?> asObjectValue() throws ValueConversionException {
        throw new ValueConversionException("Cannot convert PlaceHolder to ObjectValue");
    }

    @Override
    public boolean isSymbolic() {
        return false;
    }

    @Override
    public ImmutableSet<String> getSymbolicVariables() {
        return ImmutableSet.of();
    }

    @Override
    public LongValue asLongValue() throws NotImplementedException {
        return super.asLongValue();
    }

    @Override
    public String toString() {
        return "PlaceHolder (" + (isSymbolic ? "symbolic, " : "") + origin + ")";
    }
}
