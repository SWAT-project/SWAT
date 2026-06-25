package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.symbolic.shadow.ShadowContext
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import spock.lang.See

/**
 * O-4 / O-5 — object reference-equality and the canonical registry. Level L0, Phase 1 (G1).
 *
 * The G1 registry keys the heap by the concrete object reference (identity), so it returns one
 * canonical wrapper per concrete object and keeps distinct objects distinct even when their identity
 * hashes collide. {@code ObjectValue.IF_ACMPEQ} stays {@code this == o2}, which is correct under that
 * one-wrapper-per-identity guarantee. These specs assert both: same object → same wrapper → equal
 * (O-4); distinct objects → distinct wrappers / two entries → unequal (O-5). The faithful end-to-end
 * recovery is additionally anchored at L2 (see HeapRecoveryV1AgentSpec).
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
    def "O-4: the same concrete object recovers the same canonical wrapper (reference-equal)"() {
        given: "a shadow registered under a concrete object"
        Object obj = new Object()
        ObjectValue shadow = objectAt(0x2000)
        ShadowContext ctx = new ShadowContext()
        ctx.putToHeap(obj, shadow)

        when: "the same concrete object is looked up twice"
        def a = ctx.getFromHeap(obj)
        def b = ctx.getFromHeap(obj)

        then: "both recover the same wrapper, so they compare reference-equal"
        a.is(b)
        isValid((a as ObjectValue).IF_ACMPEQ(b as ObjectValue))
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
    def "O-5: distinct concrete objects are stored without merging (reference keying)"() {
        given: "two distinct concrete objects, with shadows that happen to share an address"
        Object o1 = new Object()
        Object o2 = new Object()
        ObjectValue a = objectAt(0x5000)
        ObjectValue b = objectAt(0x5000)

        when: "both are registered under their concrete references"
        ShadowContext shadow = new ShadowContext()
        shadow.putToHeap(o1, a)
        shadow.putToHeap(o2, b)

        then: "the reference-keyed registry keeps distinct objects as distinct entries"
        // (Structural contract of reference keying; the behavioral collision/recovery coverage is at
        // L1 (ValueRecoverySpec) and the L2 anchor.)
        shadow.heapSize() == 2
    }
}
