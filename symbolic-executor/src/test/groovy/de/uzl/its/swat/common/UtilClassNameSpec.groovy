package de.uzl.its.swat.common

import de.uzl.its.swat.common.exceptions.SWATAssert
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit test for {@link Util#formatClassName} and the private {@code checkClassName} guard behind it.
 *
 * <p>The guard's sole job is to reject <em>type descriptors</em> (object {@code Lxxx;}, method
 * {@code (..)X}) passed where a <em>class name</em> is expected — in either spelling: dotted/binary
 * ({@code a.b.C}) or internal/slashed ({@code a/b/C}). {@code formatClassName} canonicalizes to the
 * internal form.
 *
 * <p>A regression (commit a80dc60, incidental to an unrelated UF commit) added
 * {@code && !className.startsWith("L")} to the guard. Because the predicate keys on the first
 * character, it spuriously rejected every legitimate class name beginning with 'L' — notably
 * default-package targets/benchmarks named {@code L...} (e.g. {@code LitSymTarget}) — crashing
 * instrumentation through {@link SWATAssert}. Object descriptors are already caught by the {@code ';'}
 * clause, so the term was pure false-positive surface and was removed. This pins both halves of the
 * contract: L-prefixed class names round-trip, real descriptors still throw.
 */
class UtilClassNameSpec extends Specification {

    def setup() {
        // checkClassName routes failures through SWATAssert; make the guard deterministically active
        // regardless of any earlier spec that may have toggled it. The test task sets exitOnError=false,
        // so a failed assertion rethrows (catchable) rather than halting the JVM.
        SWATAssert.setEnabled(true)
    }

    @Unroll
    def "formatClassName accepts class name '#name' and canonicalizes to '#expected'"() {
        expect:
        Util.formatClassName(name) == expected

        where:
        name               || expected
        "LitSymTarget"     || "LitSymTarget"      // the regression case: default-package, L-prefixed
        "Long"             || "Long"
        "List"             || "List"
        "java.lang.Long"   || "java/lang/Long"    // dotted -> slashed
        "java/lang/String" || "java/lang/String"  // already internal -> idempotent
    }

    @Unroll
    def "formatClassName still rejects the type descriptor '#descriptor'"() {
        when:
        Util.formatClassName(descriptor)

        then:
        thrown(AssertionError)

        where:
        descriptor << ["Ljava/lang/String;", "Lfoo/Bar;", "(I)V", "(Ljava/lang/String;)V"]
    }

    def "formatClassName skips the check for array descriptors (early return, not rejected)"() {
        expect: "arrays are intentionally skipped by checkClassName, so they pass through unchanged"
        Util.formatClassName("[I") == "[I"
    }
}
