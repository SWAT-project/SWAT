package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * End-to-end check that the boxed de-intern instrumentation produces JVM-valid bytecode. The real agent
 * runs a program driving both emitted paths: the unbox+rebox wrap on un-instrumented valueOf(String)
 * returns for Integer and Long, and the valueOf(primitive)->new rewrite via autoboxing for all six
 * wrappers (Integer, Long, Short, Byte, Character, Boolean). The run completing (exit 0 plus a parsed
 * trace) is the oracle: a malformed wrap or rewrite would raise a VerifyError. Object-identity effects
 * are covered by OutputDeInternSpec and the boxed-cache equality semantics by ProvenanceRefEqualsSpec.
 */
class BoxedDeInternAgentSpec extends Specification {

    def "all six wrappers' valueOf-rewrite and Integer/Long de-intern load, verify, and run"() {
        when: "the agent runs a program driving the boxed de-intern bytecode paths"
        TraceObservation obs = AgentRun.run("targets/BoxedReturnTarget.java", "BoxedReturnTarget")

        then: "the run completed (exit 0 + parsed trace) - the unbox+rebox wrap is verifier-valid"
        obs != null

        and: "the symbolic input was designated and traced"
        !obs.inputNames.isEmpty()
    }
}
