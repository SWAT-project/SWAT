package de.uzl.its.swat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A hand-written target: reaching this method is a violation, as a failing assertion at its first
 * statement would be. Use it for sinks that should not be reachable with the inputs SWAT
 * controls (a shell command, an SQL statement, a privileged operation).
 *
 * <p>With user facts on ({@code -DuserFacts=true}, explorer {@code --user-facts}), SWAT's class
 * preparation puts {@code throw new AssertionError("@Target ...")} at the start of the method, so
 * the static analysis treats it as an assertion point and the executor reports reaching it as a
 * violation. Not for constructors (the throw would have to come after the super call).
 */
@Retention(RetentionPolicy.CLASS)
@java.lang.annotation.Target({ElementType.METHOD})
public @interface Target {
    /** What reaching the method means; part of the violation's message. */
    String value() default "";
}
