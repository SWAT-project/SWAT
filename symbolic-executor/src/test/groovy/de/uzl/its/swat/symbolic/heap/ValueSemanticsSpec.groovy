package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.symbolic.value.Value
import de.uzl.its.swat.symbolic.value.reference.ObjectValue
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue
import org.objectweb.asm.Type
import org.sosy_lab.java_smt.api.Formula

/**
 * String identity and value rules that hold by construction, without instrumentation: the copy
 * constructor, reuse of an interned literal, and concrete grounding of a symbolic value.
 */
class ValueSemanticsSpec extends BaseValueSpec {

    private Set<String> varsOf(Value v) {
        return fmgr.extractVariables(v.formula as Formula).keySet()
    }

    def "new String(s) keeps the source's symbolic formula"() {
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

    def "reusing the same literal yields the same constant formula with no free variables"() {
        given:
        StringValue a = new StringValue(context, "lit", ObjectValue.ADDRESS_UNKNOWN)
        StringValue b = new StringValue(context, "lit", ObjectValue.ADDRESS_UNKNOWN)

        expect: "both are constants (no free variables) and compare value-equal"
        varsOf(a).isEmpty()
        varsOf(b).isEmpty()
        isValid(a.IF_ACMPEQ(b))
    }

    def "making a value symbolic preserves its concrete grounding"() {
        given:
        StringValue s = new StringValue(context, "seed", ObjectValue.ADDRESS_UNKNOWN)
        s.MAKE_SYMBOLIC()

        expect:
        s.concrete == "seed"
        !varsOf(s).isEmpty()
    }
}
