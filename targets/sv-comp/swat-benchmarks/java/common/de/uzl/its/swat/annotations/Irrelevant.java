package de.uzl.its.swat.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A hand-written fact: the branches of this method (or of every method of this class) cannot
 * change whether a target is reached, nor any value a target or a relevant branch reads.
 *
 * <p>With user facts on ({@code -DuserFacts=true}, explorer {@code --user-facts}), SWAT does not
 * flip these branches and leaves their constraints out of its queries, as for the branches its own
 * relevance analysis slices. Callees are not covered; annotate them too, or use
 * {@link Swat#beginIrrelevant()} around the calls. The fact is trusted: if it is wrong, SWAT can
 * miss a violation. A SAFE verdict that relied on it says so ("SAFE assuming n user facts").
 * For statements and loops inside a method, use {@link Swat#beginIrrelevant()} and
 * {@link Swat#endIrrelevant()}.
 */
@Retention(RetentionPolicy.CLASS)
@java.lang.annotation.Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Irrelevant {
    /** Why the code is irrelevant; for the reader and the logs. */
    String value() default "";
}
