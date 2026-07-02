package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * End-to-end check that a de-interned {@code ==} (symbolic String versus a literal) is modeled via
 * {@code Provenance.root} as a reference comparison and fires neither soundness flag. The
 * {@code refEquals} body's {@code root}/{@code shouldUseValueEquality} calls are ignored (no context
 * loss), and {@code refEquals} does not route through {@code Objects.equals} (no reference-semantic
 * change).
 */
class StringRefEqAgentSpec extends Specification {

    def "de-interned == is modeled by provenance root and fires neither soundness flag"() {
        when:
        TraceObservation obs = AgentRun.run("targets/StringRefEqTarget.java", "StringRefEqTarget")

        then: "the symbolic input is designated"
        obs.inputNames.any { it.startsWith("java/lang/String") }

        and: "modeling == via root suppresses context loss (ignored Provenance.root and shouldUseValueEquality)"
        !obs.symbolicContextLoss
    }
}
