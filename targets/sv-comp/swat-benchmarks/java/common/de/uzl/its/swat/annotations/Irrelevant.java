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
 * relevance analysis slices. Everything it calls is covered too (callees, constructors, virtual
 * calls, recursion), but only when called from here: the same methods called from elsewhere stay
 * decisions (SWAT tracks the fact on its call stack). SWAT's static analysis checks the fact where
 * it can: code that contains a target, may end the program or calls code that may reach a target
 * refutes it, and it is not used. Where SWAT's relevance analysis slices a branch by itself, the
 * fact is not needed; it also lets that analysis slice branches outside the code that only feed
 * it. Otherwise the fact is trusted: if it is wrong, SWAT can miss a violation. A SAFE verdict
 * that relied on it says so ("SAFE assuming n user facts").
 * For statements and loops inside a method, use {@link Swat#beginIrrelevant()} and
 * {@link Swat#endIrrelevant()}.
 */
@Retention(RetentionPolicy.CLASS)
@java.lang.annotation.Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Irrelevant {
    /** Why the code is irrelevant; for the reader and the logs. */
    String value() default "";
}
