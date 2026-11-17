package de.uzl.its.swat.symbolic.value.reference.regex;

import de.uzl.its.swat.common.exceptions.NotImplementedException;
import de.uzl.its.swat.common.exceptions.ValueConversionException;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.reference.ObjectValue;
import de.uzl.its.swat.symbolic.value.reference.lang.JavaRegex;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 * Shadow of a <a
 * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html">Pattern</a>,
 * made from the concrete (immutable) Pattern the JVM holds, so its regular expression and flags
 * are exact. It is never symbolic.
 *
 * <p>Only {@code matcher(s)} for a symbolic String {@code s} is modelled, and only for a pattern
 * without flags whose regular expression {@link JavaRegex} translates: it returns a {@link
 * MatcherValue}. Everything else is not modelled (a PlaceHolder, as for any unmodelled call).
 */
public final class PatternValue extends ObjectValue<Object, Object> {
    private static final String PATTERN = "java/util/regex/Pattern";

    /** The concrete Pattern this shadows. */
    private final Pattern pattern;

    public PatternValue(SolverContext context, Pattern pattern, int address) {
        super(context, address);
        this.className = PATTERN;
        this.pattern = pattern;
    }

    /** Whether this shadows the concrete object {@code concrete}. */
    public boolean shadows(Object concrete) {
        return pattern == concrete;
    }

    @Override
    public Value<?, ?> invokeMethod(String name, Type[] desc, Value<?, ?>[] args)
            throws NotImplementedException, ValueConversionException {
        if (name.equals("matcher") && args.length == 1 && args[0] instanceof StringValue input
                && input.isSymbolic() && pattern.flags() == 0
                && JavaRegex.toRegex(context.getFormulaManager().getStringFormulaManager(), pattern.pattern()) != null) {
            return new MatcherValue(context, pattern, input);
        }
        return super.invokeMethod(name, desc, args);
    }

    @Override
    public String toString() {
        return "PatternValue @" + Integer.toHexString(address) + " { " + pattern.pattern() + " }";
    }
}
