/**
 * Loop-head state matching: at the target of every backward jump the executor hashes the program
 * state, provided no part of it is symbolic, and reports the hash with the next branch. The
 * explorer does not flip a branch that follows a state it has already reached on a shorter (or
 * equally long, but earlier ordered) path: whatever that branch could still lead to is reachable
 * from the earlier occurrence as well (see LoopStateMatcher).
 */
package de.uzl.its.swat.symbolic.loopstate;
