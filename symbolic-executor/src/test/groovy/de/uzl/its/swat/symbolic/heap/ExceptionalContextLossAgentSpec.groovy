package de.uzl.its.swat.symbolic.heap

import de.uzl.its.swat.testsupport.agent.AgentRun
import de.uzl.its.swat.testsupport.agent.TraceObservation
import spock.lang.Specification

/**
 * End-to-end contract for the exception path of non-instrumented callees. A JDK call that throws
 * as a function of a symbolic input (Float.parseFloat on a non-numeric symbolic String) must record
 * symbolic context loss - the thrown-or-not decision is control flow the symbolic layer did not
 * model, so a SAFE verdict would be unsound. A throw raised inside an INSTRUMENTED callee must NOT
 * flag: its guarding branch is symbolically tracked, so the decision IS modeled.
 *
 * <p>Both targets catch their exceptions (AgentRun requires exit 0).
 */
class ExceptionalContextLossAgentSpec extends Specification {

    def "a JDK call throwing on symbolic input records context loss"() {
        when: "the agent runs parseFloat on a symbolic, non-numeric String (throws, caught)"
        TraceObservation obs = AgentRun.run("targets/ExceptionalThrowTarget.java", "ExceptionalThrowTarget")

        then: "the run completed and the symbolic input was designated"
        obs != null
        !obs.inputNames.isEmpty()

        and: "the unexplored throw decision blocks SAFE"
        obs.symbolicContextLoss
    }

    def "a throw inside an instrumented callee records no context loss"() {
        when: "the agent runs a symbolic-guarded throw raised by an instrumented helper (caught)"
        TraceObservation obs = AgentRun.run("targets/InstrumentedThrowTarget.java", "InstrumentedThrowTarget")

        then: "the run completed and the symbolic input was designated"
        obs != null
        !obs.inputNames.isEmpty()

        and: "the throw decision is modeled by the tracked branch, so nothing flags"
        !obs.symbolicContextLoss
    }
}
