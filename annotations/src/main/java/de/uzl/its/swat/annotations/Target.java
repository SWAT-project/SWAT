package de.uzl.its.swat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A hand-written target: reaching this method is a violation, as a failing assertion at its first
 * statement would be. Use it for sinks that should not be reachable with the inputs SWAT
 * controls (a shell command, an SQL statement, a privileged operation).
 *
 * <p>With user facts on ({@code -DuserFacts=true}, explorer {@code --user-facts}), SWAT's rewrite
 * starts the method with a call that throws {@code AssertionError("@Target ...")}, so the executor
 * reports reaching it as a violation; the static analysis (the extractor and the property
 * pre-check) reads the annotation itself and treats the method's entry as an assertion point. Not
 * for constructors (the call would have to come after the super call).
 */
@Retention(RetentionPolicy.CLASS)
@java.lang.annotation.Target({ElementType.METHOD})
public @interface Target {
    /** What reaching the method means; part of the violation's message. */
    String value() default "";
}
