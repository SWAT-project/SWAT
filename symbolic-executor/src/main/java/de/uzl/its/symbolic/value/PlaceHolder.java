package de.uzl.its.symbolic.value;

import de.uzl.its.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.symbolic.value.reference.ObjectValue;
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
        DATABASE
    }

    public final boolean isSymbolic;
    public final ValueOrigin origin;
    public static final PlaceHolder instance = new PlaceHolder(false);
    public static final PlaceHolder symbolicInstance = new PlaceHolder(true);

    public PlaceHolder(boolean isSymbolic) {
        this.isSymbolic = isSymbolic;
        this.origin = ValueOrigin.UNSPECIFIED;
    }

    public PlaceHolder(boolean isSymbolic, ValueOrigin origin) {
        this.isSymbolic = isSymbolic;
        this.origin = origin;
    }

    public ObjectValue<?, ?> asObjectValue() {
        throw new RuntimeException("Cannot convert PlaceHolder to ObjectValue");
        // return ObjectValue.NULL;
    }

    @Override
    public LongValue asLongValue() {
        return super.asLongValue();
    }

    @Override
    public String toString() {
        return "PlaceHolder{\"isSymbolic\":\"" + isSymbolic + "\", \"origin\":\"" + origin + "\"}";
    }
}
