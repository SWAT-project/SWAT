package de.uzl.its.swat.symbolic.shadow;

/**
 * Policy for handling a shadow/concrete divergence detected at a GETVALUE sync point: the shadow
 * value's concrete no longer matches the value the real JVM produced (an out-of-band change, e.g. an
 * object mutated inside unmodeled code). Configured via {@code shadow.divergence}.
 */
public enum ShadowDivergence {
    /**
     * Hard-fail on divergence (SWATAssert) - preserves the original behavior, which is useful for
     * catching executor-internal desync bugs in dev/CI. Default.
     */
    CRASH,

    /**
     * Detect gracefully: record a soundness flag (context loss -> SAFE downgraded to UNKNOWN), adopt
     * the observed concrete value, and continue. Fully sound; recommended for SV-COMP / production
     * runs (no spurious crashes). Until escape-aware differentiation lands (G4a), this does not
     * distinguish a legitimate out-of-band change from an executor desync - both are flagged.
     */
    FLAG
}
