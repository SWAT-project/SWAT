package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.See
import spock.lang.Specification

/**
 * G3-B end-to-end acceptance (Level L2): a de-interned {@code ==} (symbolic String vs a literal) is
 * modeled via Provenance.root as a reference comparison, and must fire NEITHER soundness flag. This
 * pins the round-2 mechanism correction: the stepped {@code refEquals} body's {@code root}/
 * {@code shouldUseValueEquality} calls are IGNORED (no context loss), and {@code refEquals} no longer
 * routes through {@code Objects.equals} (no reference-semantic-change). The old value-equality
 * {@code refEquals} fired both flags here. Mirrors PureFunctionUFAgentSpec.
 */
class StringRefEqAgentSpec extends Specification {

    @See("docs/heap-redesign-g3-design.md")
    def "G3-B (L2): de-interned == is modeled by provenance root and fires neither soundness flag"() {
        when:
        TraceObservation obs = AgentRun.run("targets/StringRefEqTarget.java", "StringRefEqTarget")

        then: "the symbolic input is designated"
        obs.inputNames.any { it.startsWith("java/lang/String") }

        and: "modeling == via root fires NEITHER flag (the over-firing fix; old refEquals fired both)"
        !obs.symbolicContextLoss
        !obs.referenceSemanticChange
    }
}
