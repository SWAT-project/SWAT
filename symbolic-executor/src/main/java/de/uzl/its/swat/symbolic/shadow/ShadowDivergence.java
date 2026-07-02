package de.uzl.its.swat.symbolic.shadow;

/**
 * Policy for handling a shadow/concrete divergence detected at a GETVALUE sync point: the shadow
 * value's concrete no longer matches the value the real JVM produced (an out-of-band change, e.g. an
 * object mutated inside unmodeled code). Configured via {@code shadow.divergence}.
 */
public enum ShadowDivergence {
    /**
     * Hard-fail on divergence (SWATAssert). Useful for catching executor-internal desync bugs in
     * development and CI. Default.
     */
    CRASH,

    /**
     * Detect gracefully: record a soundness flag (context loss, which downgrades a SAFE verdict to
     * UNKNOWN), adopt the observed concrete value, and continue. Fully sound; recommended for SV-COMP
     * and production runs (no spurious crashes). This policy does not distinguish a legitimate
     * out-of-band change from an executor desync - both are flagged.
     */
    FLAG
}
