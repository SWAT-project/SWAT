package de.uzl.its.swat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A hand-written fact: library code never calls this method (it is no callback), although it
 * overrides or implements a library method, or is the target of a lambda or method reference.
 *
 * <p>SWAT's static analysis must otherwise assume that any library call may run it; if it may reach
 * a target, every library call becomes a target point, and little can be pruned. With user facts on
 * (explorer {@code --user-facts}), the static analysis leaves the method out of the callbacks. The
 * fact is checked on every run: a library call that does call it back refutes it, and the verdict
 * cannot be SAFE. A SAFE verdict otherwise says it assumed the fact.
 */
@Retention(RetentionPolicy.CLASS)
@java.lang.annotation.Target({ElementType.METHOD})
public @interface NotCalledBack {
    /** Why library code never calls it (documentation only). */
    String value() default "";
}
