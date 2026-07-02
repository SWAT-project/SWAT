package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.symbolic.shadow.ShadowContext
import de.uzl.its.swat.symbolic.value.primitive.numeric.integral.IntValue
import de.uzl.its.swat.symbolic.value.reference.ObjectValue

/**
 * Object reference-equality and the canonical registry. The registry keys the heap by the concrete
 * object reference (identity), so it returns one canonical wrapper per concrete object and keeps
 * distinct objects distinct even when their identity hashes collide. {@code ObjectValue.IF_ACMPEQ}
 * stays {@code this == o2}, which is correct under that one-wrapper-per-identity guarantee. These
 * specs assert both: the same object recovers the same wrapper and compares equal, while distinct
 * objects stay distinct wrappers and compare unequal.
 */
class ObjectIdentitySpec extends BaseValueSpec {

    private ObjectValue objectAt(int address) {
        return new ObjectValue(context, "de/uzl/its/swat/test/Obj", new IntValue(context, 1), address)
    }

    def "two references to distinct objects compare reference-unequal"() {
        given:
        ObjectValue a = objectAt(0x2000)
        ObjectValue c = objectAt(0x3000)

        expect:
        isUnsatisfiable(a.IF_ACMPEQ(c))
    }

    def "the same concrete object recovers the same canonical wrapper (reference-equal)"() {
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

    def "distinct objects with a colliding identity hash compare reference-unequal"() {
        given: "two distinct objects whose identity hash (address) collides"
        ObjectValue a = objectAt(0x5000)
        ObjectValue b = objectAt(0x5000)

        expect: "they are still distinct references"
        isUnsatisfiable(a.IF_ACMPEQ(b))
    }

    def "distinct concrete objects are stored without merging (reference keying)"() {
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
        shadow.heapSize() == 2
    }
}
