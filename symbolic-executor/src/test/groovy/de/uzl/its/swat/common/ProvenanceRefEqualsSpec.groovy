package de.uzl.its.swat.common

import spock.lang.See
import spock.lang.Specification

/**
 * G3-B unit test: {@link UtilInstrumented#refEquals} models reference equality by ORIGINAL identity via
 * the {@link Provenance} map. Two de-interned copies that root to the same canonical compare equal;
 * distinct canonicals compare unequal; non-de-interned classes fall back to plain reference equality.
 * This pins the executor-independent core of G3-B (the de-intern bytecode that populates the map is
 * exercised at L2). See docs/heap-redesign-g3-design.md.
 */
class ProvenanceRefEqualsSpec extends Specification {

    @See("docs/heap-redesign-g3-design.md")
    def "de-interned Strings rooting to the same interned canonical compare equal"() {
        given: "two distinct de-interned copies of the same literal, both rooted to the interned canonical"
        String canonical = "g3b-abc".intern()
        String a = new String("g3b-abc")
        String b = new String("g3b-abc")
        Provenance.record(a, canonical)
        Provenance.record(b, canonical)

        expect: "they are distinct objects (so a==b reference would be false)"
        !a.is(b)
        and: "but refEquals compares roots -> same canonical -> equal (matches real interned ==)"
        UtilInstrumented.refEquals(a, b)
    }

    @See("docs/heap-redesign-g3-design.md")
    def "a de-interned copy vs a same-valued object with a different root compares unequal"() {
        given: "a rooted to the interned canonical; b has no provenance entry (root(b)=b)"
        String a = new String("g3b-x")
        Provenance.record(a, "g3b-x".intern())
        String b = new String("g3b-x")

        expect: "distinct roots -> unequal (matches real new String(\"x\") == \"x\" -> false)"
        !UtilInstrumented.refEquals(a, b)
    }

    @See("docs/heap-redesign-g3-design.md")
    def "boxed copies rooting to the cached canonical compare equal (cache range)"() {
        given:
        Integer a = new Integer(100)
        Integer b = new Integer(100)
        Provenance.record(a, Integer.valueOf(100))
        Provenance.record(b, Integer.valueOf(100))

        expect: "both root to the one cached Integer(100) -> equal (matches real valueOf(100)==valueOf(100))"
        !a.is(b)
        UtilInstrumented.refEquals(a, b)
    }

    @See("docs/heap-redesign-g3-design.md")
    def "non-de-interned classes use plain reference equality"() {
        given:
        Object a = new Object()
        Object b = new Object()

        expect: "Object is not a de-interned class -> refEquals falls back to a==b"
        !UtilInstrumented.refEquals(a, b)
        UtilInstrumented.refEquals(a, a)
    }

    @See("docs/heap-redesign-g3-design.md")
    def "root collapses chains at insert"() {
        given: "c rooted to b, b rooted to canonical -> root(c) must resolve to canonical"
        String canonical = "g3b-chain".intern()
        String b = new String("g3b-chain")
        Provenance.record(b, canonical)
        String c = new String("g3b-chain")
        Provenance.record(c, b)

        expect:
        Provenance.root(c).is(canonical)
    }
}
