package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import org.objectweb.asm.Type
import org.sosy_lab.java_smt.api.Formula
import spock.lang.See

/**
 * Value-typed semantics at Level L0 — String identity/value rules that hold by construction
 * (no instrumentation). V-5 (copy ctor), V-6 (interned-literal reuse), V-9 (concrete grounding).
 * These document currently-correct behavior and guard against regression. See
 * docs/heap-redesign-tests.md.
 */
class ValueSemanticsSpec extends BaseValueSpec {

    private Set<String> varsOf(Value v) {
        return fmgr.extractVariables(v.formula as Formula).keySet()
    }

    @See("docs/heap-redesign-tests.md")
    def "V-5: new String(s) keeps the source's symbolic formula (committed copy-ctor model)"() {
        given: "a symbolic source string and a fresh target"
        StringValue s = new StringValue(context, "abc", ObjectValue.ADDRESS_UNKNOWN)
        s.MAKE_SYMBOLIC()
        StringValue t = new StringValue(context, "abc", ObjectValue.ADDRESS_UNKNOWN)

        when: "t = new String(s) via the copy constructor"
        Type[] desc = [Type.getType("Ljava/lang/String;")] as Type[]
        t.invokeMethod("<init>", desc, [s] as Value[])

        then: "the content copy carries the same symbolic value"
        !varsOf(s).isEmpty()
        varsOf(t) == varsOf(s)
    }

    @See("docs/heap-redesign-tests.md")
    def "V-6: reusing the same literal yields the same constant formula with no spurious vars"() {
        given:
        StringValue a = new StringValue(context, "lit", ObjectValue.ADDRESS_UNKNOWN)
        StringValue b = new StringValue(context, "lit", ObjectValue.ADDRESS_UNKNOWN)

        expect: "both are constants (no free variables) and compare value-equal"
        varsOf(a).isEmpty()
        varsOf(b).isEmpty()
        isValid(a.IF_ACMPEQ(b))
    }

    @See("docs/heap-redesign-tests.md")
    def "V-9: making a value symbolic preserves its concrete grounding"() {
        given:
        StringValue s = new StringValue(context, "seed", ObjectValue.ADDRESS_UNKNOWN)
        s.MAKE_SYMBOLIC()

        expect:
        s.concrete == "seed"
        !varsOf(s).isEmpty()
    }
}
