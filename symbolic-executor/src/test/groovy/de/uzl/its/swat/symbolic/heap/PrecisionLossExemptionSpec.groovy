package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.symbolic.trace.DTOBuilder
import org.sosy_lab.java_smt.api.BooleanFormula
import org.sosy_lab.java_smt.api.Formula
import org.sosy_lab.java_smt.api.FormulaType
import org.sosy_lab.java_smt.api.StringFormula

/**
 * Precision-loss exemption for pure uninterpreted functions. {@link DTOBuilder#isPrecisionLoss}
 * treats a whitelisted generic {@code pure_} UF over designated inputs as precision-preserving (an
 * axiom-free UF over inputs over-approximates any deterministic function, so UNSAT under the free UF
 * implies real UNSAT and SAFE holds), while a non-input variable or a bespoke (non-{@code pure_}) UF
 * still loses precision. Input-ness is decided by exact term identity against the designated inputs
 * rather than a name pattern, so it also holds for String inputs named {@code java/lang/String_0}.
 */
class PrecisionLossExemptionSpec extends BaseValueSpec {

    // A realistic String input variable name: lowercase plus slashes.
    private StringFormula inputVar() { fmgr.getStringFormulaManager().makeVariable("java/lang/String_0") }
    private StringFormula otherVar() { fmgr.getStringFormulaManager().makeVariable("intermediate") }

    private Set<Formula> inputs(StringFormula... terms) { return terms.toList() as Set }

    private StringFormula ufOver(String ufName, StringFormula arg) {
        def decl = fmgr.getUFManager().declareUF(ufName, FormulaType.StringType, FormulaType.StringType)
        return (StringFormula) fmgr.getUFManager().callUF(decl, arg)
    }

    private BooleanFormula eqAbc(StringFormula s) {
        return fmgr.getStringFormulaManager().equal(s, fmgr.getStringFormulaManager().makeString("abc"))
    }

    def "a designated input variable alone is not precision loss, including a String input"() {
        given:
        def v = inputVar()
        expect:
        !DTOBuilder.isPrecisionLoss(eqAbc(v), fmgr, inputs(v))
    }

    def "a non-input variable is precision loss"() {
        expect:
        DTOBuilder.isPrecisionLoss(eqAbc(otherVar()), fmgr, inputs(inputVar()))
    }

    def "a pure_ UF over an input variable is NOT precision loss (the exemption)"() {
        given:
        def v = inputVar()
        expect:
        !DTOBuilder.isPrecisionLoss(eqAbc(ufOver("pure_String_trim", v)), fmgr, inputs(v))
    }

    def "a pure_ UF over a NON-input variable is still precision loss (transitivity)"() {
        expect:
        DTOBuilder.isPrecisionLoss(eqAbc(ufOver("pure_String_trim", otherVar())), fmgr, inputs(inputVar()))
    }

    def "a bespoke (non-pure_) UF stays non-exempt (precision loss)"() {
        given:
        def v = inputVar()
        expect:
        DTOBuilder.isPrecisionLoss(eqAbc(ufOver("toLowerCase", v)), fmgr, inputs(v))
    }
}
