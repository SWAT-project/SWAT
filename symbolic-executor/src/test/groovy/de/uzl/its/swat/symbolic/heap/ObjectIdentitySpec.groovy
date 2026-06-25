package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.symbolic.shadow.ShadowContext
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import spock.lang.PendingFeature
import spock.lang.See

/**
 * O-4 — reference-equality correctness. Level L0, Phase 1 (G1 canonical heap).
 *
 * Two shadow wrappers for the SAME concrete object (a duplicate registration) must compare
 * reference-equal. {@code ObjectValue.IF_ACMPEQ} currently uses {@code this == o2} on the wrapper,
 * so duplicate wrappers wrongly compare unequal — a correctness defect the canonical registry fixes.
 *
 * Convention: the "already holds" feature has no {@code @PendingFeature} and guards against
 * infra breakage; the desired-but-unimplemented behavior is marked {@code @PendingFeature} (red).
 */
class ObjectIdentitySpec extends BaseValueSpec {

    private ObjectValue objectAt(int address) {
        return new ObjectValue(context, "de/uzl/its/swat/test/Obj", new IntValue(context, 1), address)
    }

    @See("docs/heap-redesign-tests.md")
    def "O-4: two references to distinct objects compare reference-unequal"() {
        given:
        ObjectValue a = objectAt(0x2000)
        ObjectValue c = objectAt(0x3000)

        expect:
        isUnsatisfiable(a.IF_ACMPEQ(c))
    }

    @See("docs/heap-redesign-tests.md")
    @PendingFeature(reason = "G1 canonical registry not yet implemented; IF_ACMPEQ uses this==o2, so duplicate wrappers for one identity compare unequal")
    def "O-4: two wrappers for the same identity compare reference-equal"() {
        given: "two distinct wrappers sharing one identity (the duplicate-registration bug)"
        ObjectValue a = objectAt(0x2000)
        ObjectValue b = objectAt(0x2000)

        expect: "reference equality must hold for the same identity (RED until G1)"
        isValid(a.IF_ACMPEQ(b))
    }

    @See("docs/heap-redesign-tests.md")
    def "O-5: distinct objects with a colliding identity hash compare reference-unequal"() {
        given: "two distinct objects whose identity hash (address) collides"
        ObjectValue a = objectAt(0x5000)
        ObjectValue b = objectAt(0x5000)

        expect: "they are still distinct references"
        isUnsatisfiable(a.IF_ACMPEQ(b))
    }

    @See("docs/heap-redesign-tests.md")
    @PendingFeature(reason = "G1 faithful key not implemented; the identity-hash-keyed heap merges distinct objects that share an identity hash")
    def "O-5: the heap stores colliding-hash objects without merging them"() {
        given: "two distinct objects whose identity hash (address) collides"
        int collidingHash = 0x5000
        ObjectValue a = objectAt(collidingHash)
        ObjectValue b = objectAt(collidingHash)

        when: "both are registered on the recovery heap"
        ShadowContext shadow = new ShadowContext()
        shadow.putToHeap(collidingHash, a)
        shadow.putToHeap(collidingHash, b)

        then: "both survive distinctly (RED until G1: the hash key collapses them to one)"
        shadow.heapSize() == 2
    }
}
