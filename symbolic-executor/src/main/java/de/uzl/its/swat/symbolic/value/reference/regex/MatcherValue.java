package de.uzl.its.swat.symbolic.value.reference.regex;

import com.google.common.collect.ImmutableSet;
import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Shadow of a <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Matcher.html">Matcher</a>
 * that {@link PatternValue} made for a symbolic String: the pattern (no flags, translatable by
 * JavaRegex) and the input (an immutable String).
 *
 * <p>Only {@code matches()} is modelled, as {@code input.matches(regex)}, which is what
 * String.matches does. It depends on the input, the region and the pattern only, not on an
 * earlier match. Any other call (find, group, reset, region, usePattern, ...) is not modelled: it
 * returns a PlaceHolder, which records the context loss (the matcher is symbolic), and from then
 * on matches() is not modelled either, since the call may have changed the input, region or
 * pattern. Nor is it before the shadow is bound to the JVM's Matcher (see {@link #bind}).
 */
public final class MatcherValue extends ObjectValue<Object, Object> {
    private static final String MATCHER = "java/util/regex/Matcher";
    private static final Type[] MATCHES_DESC = {Type.getType(String.class)};

    private final Pattern pattern;
    private final StringValue input;

    /** Whether a call SWAT does not model may have changed the matcher. */
    private boolean unknown = false;

    /** The concrete Matcher this shadows, once the JVM returned it. */
    private Matcher matcher;

    public MatcherValue(SolverContext context, Pattern pattern, StringValue input) {
        super(context, ObjectValue.ADDRESS_UNKNOWN);
        this.className = MATCHER;
        this.pattern = pattern;
        this.input = input;
    }

    /**
     * Binds this shadow to the Matcher the JVM returned for it. If that is not a matcher of the
     * same pattern (it always should be), the matcher is not modelled.
     */
    public void bind(Object concrete) {
        if (matcher == null && concrete instanceof Matcher m && m.pattern() == pattern) {
            matcher = m;
        } else {
            unknown = true;
        }
    }

    /** Whether this shadows the concrete object {@code concrete}. */
    public boolean shadows(Object concrete) {
        return matcher != null && matcher == concrete;
    }

    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args)
            throws NotImplementedException, ValueConversionException {
        if (!unknown && matcher != null && name.equals("matches") && args.length == 0) {
            return input.invokeMethod("matches", MATCHES_DESC,
                    new Value[] {new StringValue(context, pattern.pattern(), ObjectValue.ADDRESS_UNKNOWN)});
        }
        unknown = true;
        return PlaceHolder.instance;
    }

    @Override
    public boolean isSymbolic() {
        return input.isSymbolic();
    }

    @Override
    public boolean isSymbolic(Set<Value<?, ?>> visited) {
        return isSymbolic();
    }

    @Override
    public ImmutableSet<String> getSymbolicVariables() {
        return input.getSymbolicVariables();
    }

    @Override
    public String toString() {
        return "MatcherValue @" + Integer.toHexString(address) + " { " + pattern.pattern()
                + (unknown ? ", unknown" : "") + " }";
    }
}
