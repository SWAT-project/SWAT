package de.uzl.its.swat.common

import spock.lang.Specification

/**
 * Concrete semantics of the IF_ACMP replacement: on objects de-interned the way the NoCache
 * transformer does it, refEquals must give the answer the original program's {@code ==} gives.
 */
class RefEqualsSpec extends Specification {

    /** What the NoCache transformer emits for a string literal. */
    private static String literal(String s) {
        String copy = new String(s)
        RefIdentity.markDeInterned(copy)
        return copy
    }

    /** What the NoCache transformer emits for Integer.valueOf. */
    private static Integer boxed(int v) {
        Integer copy = new Integer(v)
        RefIdentity.markDeInterned(copy)
        return copy
    }

    def "two de-interned copies of one literal are the same instance"() {
        expect:
        UtilInstrumented.refEquals(literal("test"), literal("test"))
        !UtilInstrumented.refEquals(literal("test"), literal("other"))
    }

    def "a string built with new String is never a literal"() {
        given:
        String s1 = new String(literal("test"))

        expect:
        !UtilInstrumented.refEquals(s1, literal("test"))
        UtilInstrumented.refEquals(s1, s1)
    }

    def "new String of an input differs from the input"() {
        given:
        String input = new String("fixed".toCharArray())
        String s1 = new String(input)

        expect:
        !UtilInstrumented.refEquals(s1, input)
        UtilInstrumented.refEquals(input, input)
    }

    def "a pooled string from uninstrumented code equals a literal of its value"() {
        expect:
        UtilInstrumented.refEquals(Boolean.toString(true), literal("true"))
        !UtilInstrumented.refEquals(Boolean.toString(false), literal("true"))
    }

    def "intern returns the instance a literal of the same value stands for"() {
        given:
        String built = new StringBuilder("te").append("st").toString()

        expect:
        UtilInstrumented.refEquals(UtilInstrumented.intern(built), literal("test"))
        !UtilInstrumented.refEquals(built, literal("test"))
    }

    def "boxes share an instance only inside the cache range"() {
        expect:
        UtilInstrumented.refEquals(boxed(v), boxed(v)) == shared

        where:
        v     || shared
        0     || true
        127   || true
        -128  || true
        128   || false
        -129  || false
        1000  || false
    }

    def "a box is identical to itself outside the cache range"() {
        given:
        Integer b = boxed(1000)

        expect:
        UtilInstrumented.refEquals(b, b)
    }

    def "boxes created with new are distinct"() {
        expect:
        !UtilInstrumented.refEquals(new Integer(5), new Integer(5))
        !UtilInstrumented.refEquals(new Integer(5), boxed(5))
        UtilInstrumented.refEquals(Integer.valueOf(5), boxed(5))
    }

    def "boxes of different types or with null"() {
        expect:
        !UtilInstrumented.refEquals(boxed(5), Long.valueOf(5))
        !UtilInstrumented.refEquals(boxed(5), null)
        UtilInstrumented.refEquals(null, null)
        !UtilInstrumented.refEquals(literal("a"), null)
    }

    def "other objects compare by identity"() {
        given:
        Object o = new Object()

        expect:
        UtilInstrumented.refEquals(o, o)
        !UtilInstrumented.refEquals(o, new Object())
    }
}
