package de.uzl.its.swat.symbolic.value;

import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.instruction.Instruction;
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.LongValue;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import java.util.Map;
import org.sosy_lab.java_smt.api.Formula;

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
        GETSTATIC,
        // The return value of an unmodeled method (tagged in InvocationHandler). At recovery, a result
        // with this origin is NOT identity-recovered (which would re-bind the receiver). If it carries a
        // pure_<sig> UF (recoveredFormula, a whitelisted pure method returning a String or primitive) it
        // is modeled as that UF; otherwise a value-typed result is concretized.
        UNMODELED_RETURN
    }

    public final boolean isSymbolic;
    public final ValueOrigin origin;
    public final Instruction inst;
    public final ObjectValue<?, ?> referenceValue;
    /**
     * For an UNMODELED_RETURN placeholder of a whitelisted pure method: the generic UF formula
     * {@code pure_<sig>(inputs)} modeling the result. Null otherwise (recovery then concretizes).
     */
    public final Formula recoveredFormula;
    /**
     * The same generic UF applied to the CONSTANT (observed) inputs, e.g.
     * {@code pure_<sig>(makeString(concreteInput))}. At recovery this is asserted equal to the
     * observed concrete output to record a ground (input -> output) pair. Null when no pair is
     * emitted (non-String inputs, or not a whitelisted pure call).
     */
    public final Formula observedApplication;
    public static final PlaceHolder instance = new PlaceHolder(false);
    public static final PlaceHolder symbolicInstance = new PlaceHolder(true);

    public PlaceHolder(boolean isSymbolic) {
        this.isSymbolic = isSymbolic;
        this.origin = ValueOrigin.UNSPECIFIED;
        this.inst = null;
        this.referenceValue = null;
        this.recoveredFormula = null;
        this.observedApplication = null;
    }

    public PlaceHolder(ValueOrigin origin, Instruction inst, ObjectValue<?, ?> referenceValue) {
        this.origin = origin;
        this.isSymbolic = false;
        this.inst = inst;
        this.referenceValue = referenceValue;
        this.recoveredFormula = null;
        this.observedApplication = null;
    }

    public PlaceHolder(boolean isSymbolic, ValueOrigin origin) {
        this.isSymbolic = isSymbolic;
        this.origin = origin;
        this.inst = null;
        this.referenceValue = null;
        this.recoveredFormula = null;
        this.observedApplication = null;
    }

    /**
     * UNMODELED_RETURN placeholder. {@code referenceValue} is the receiver of the unmodeled call
     * (null for static calls); recovery uses it to tell a returned distinct tracked value from a
     * this-return. For a whitelisted pure method it also carries the generic UF over the symbolic
     * inputs ({@code recoveredFormula}, modeling the result) and the same UF over the constant
     * observed inputs ({@code observedApplication}, used to record the observed pair). Any of these
     * may be null.
     */
    public PlaceHolder(ValueOrigin origin, ObjectValue<?, ?> referenceValue,
            Formula recoveredFormula, Formula observedApplication) {
        this.origin = origin;
        this.isSymbolic = false;
        this.inst = null;
        this.referenceValue = referenceValue;
        this.recoveredFormula = recoveredFormula;
        this.observedApplication = observedApplication;
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
