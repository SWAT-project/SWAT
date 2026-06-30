package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.symbolic.value.ValueFactory
import de.uzl.its.swat.symbolic.value.ValueType
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.DoubleValue
import de.uzl.its.swat.symbolic.value.primitive.numeric.floatingpoint.FloatValue
import spock.lang.See
import spock.lang.Unroll

/**
 * G4 primitive-return recovery unit (Level L0): {@link ValueFactory#createNumericalValue(ValueType,
 * Object, org.sosy_lab.java_smt.api.Formula)} builds a primitive value carrying an explicit symbolic
 * formula (concrete = observed), for EVERY primitive sort. This is the construction the primitive
 * GETVALUE recovery uses to install a {@code pure_<sig>} UF result. It pins the per-sort cast and that
 * a carried symbolic formula makes the value symbolic - including short and byte, which have no pure
 * unmodeled JDK method to exercise at L2. The sort matching of int/long/double/float/char/boolean is
 * additionally anchored end-to-end by PureFunctionPrimitiveAgentSpec.
 */
class PureUFPrimitiveRecoverySpec extends BaseValueSpec {

    @See("docs/heap-redesign-g4-whitelist-survey.md")
    @Unroll
    def "createNumericalValue installs a symbolic #type formula (concrete=#concrete)"() {
        given: "a free variable formula of the matching sort (stands in for a pure_<sig> UF application)"
        def formula = mk.call(context)

        when: "the value is built from the observed concrete + the symbolic formula"
        def v = ValueFactory.createNumericalValue(type, concrete, formula)

        then: "the concrete is the observed value"
        v.concrete == concrete

        and: "the carried formula makes the value symbolic and exposes the input variable"
        v.isSymbolic()
        v.getSymbolicVariables().contains(varName)

        where:
        type                   | concrete             | varName   | mk
        ValueType.intValue     | (5 as Integer)       | "v_int"   | { c -> c.formulaManager.bitvectorFormulaManager.makeVariable(32, "v_int") }
        ValueType.longValue    | (5L as Long)         | "v_long"  | { c -> c.formulaManager.bitvectorFormulaManager.makeVariable(64, "v_long") }
        ValueType.shortValue   | (5 as Short)         | "v_short" | { c -> c.formulaManager.bitvectorFormulaManager.makeVariable(16, "v_short") }
        ValueType.byteValue    | (5 as Byte)          | "v_byte"  | { c -> c.formulaManager.bitvectorFormulaManager.makeVariable(8, "v_byte") }
        ValueType.charValue    | ('a' as Character)   | "v_char"  | { c -> c.formulaManager.bitvectorFormulaManager.makeVariable(16, "v_char") }
        ValueType.booleanValue | (true as Boolean)    | "v_bool"  | { c -> c.formulaManager.booleanFormulaManager.makeVariable("v_bool") }
        ValueType.floatValue   | (1.5f as Float)      | "v_float" | { c -> c.formulaManager.floatingPointFormulaManager.makeVariable("v_float", FloatValue.precision) }
        ValueType.doubleValue  | (2.5d as Double)     | "v_dbl"   | { c -> c.formulaManager.floatingPointFormulaManager.makeVariable("v_dbl", DoubleValue.precision) }
    }
}
