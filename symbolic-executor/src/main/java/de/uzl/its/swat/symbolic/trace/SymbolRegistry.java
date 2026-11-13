package de.uzl.its.swat.symbolic.trace;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records, where a symbol is declared, what kind of symbol it is. The {@link DTOBuilder} uses it
 * to decide whether a branch constraint is encoded precisely: a constraint over a symbol that is
 * approximate, or that was never declared here, is a precision loss.
 *
 * <p>A precise symbol may still be under-constrained (a fresh value, an array base outside the
 * stored range, a partially defined model function). That only over-approximates: an UNSAT query
 * stays a proof, and a guessed model that does not reach its branch is caught as a divergence.
 * A model whose constraints may exclude real behaviour must be declared {@link
 * Kind#APPROXIMATE_MODEL_FUNCTION}, and must also call {@link
 * SymbolicTraceHandler#recordSymbolicPrecisionLoss()} where it adds such a constraint, since the
 * constraint reaches every query whether or not a branch mentions the function.
 */
public final class SymbolRegistry {

    public enum Kind {
        /** A Verifier input, solved for by the explorer. */
        INPUT,
        /** A symbolic value of the program that is not an input (e.g. a havocked field). */
        VALUE,
        /** The base symbol of an allocated array, overwritten by its initial contents. */
        ARRAY_BASE,
        /** A model function whose definition holds for every argument. */
        MODEL_FUNCTION,
        /** A model function whose encoding may exclude real behaviour. */
        APPROXIMATE_MODEL_FUNCTION
    }

    private static final Map<String, Kind> kinds = new ConcurrentHashMap<>();

    private SymbolRegistry() {}

    /**
     * Declares the kind of a symbol. An input stays an input; an approximate model function stays
     * approximate.
     */
    public static void declare(String name, Kind kind) {
        kinds.merge(name, kind, (old, k) ->
                old == Kind.INPUT || old == Kind.APPROXIMATE_MODEL_FUNCTION ? old : k);
    }

    /** Returns the kind of the symbol, or null if it was never declared. */
    public static Kind kindOf(String name) {
        return kinds.get(name);
    }

    /** Whether constraints over the symbol are encoded precisely (see the class comment). */
    public static boolean isPrecise(String name) {
        Kind kind = kinds.get(name);
        return kind != null && kind != Kind.APPROXIMATE_MODEL_FUNCTION;
    }
}
