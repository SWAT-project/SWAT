package de.uzl.its.swat.testsupport.agent

import com.fasterxml.jackson.databind.JsonNode

/**
 * A structured view of a {@code TraceDTO} emitted by a Level-2 agent run (solver.mode=PRINT).
 * Holds only what tests assert on: the soundness flags, the symbolic input names, and the
 * (non-null) branch constraint strings. See docs/test-architecture.md (Level L2).
 */
class TraceObservation {

    boolean symbolicContextLoss
    boolean symbolicPrecisionLoss
    boolean referenceSemanticChange
    List<String> inputNames = []
    List<String> branchConstraints = []

    static TraceObservation parse(JsonNode root) {
        TraceObservation o = new TraceObservation()
        o.symbolicContextLoss = root.path("symbolicContextLoss").asBoolean()
        o.symbolicPrecisionLoss = root.path("symbolicPrecisionLoss").asBoolean()
        o.referenceSemanticChange = root.path("referenceSemanticChange").asBoolean()
        root.path("inputs").each { o.inputNames << it.path("name").asText() }
        root.path("trace").each {
            JsonNode c = it.path("constraint")
            if (c != null && c.isTextual()) {
                o.branchConstraints << c.asText()
            }
        }
        return o
    }

    /** Whether any branch constraint mentions {@code token} (e.g. a symbolic input variable name). */
    boolean anyBranchReferences(String token) {
        return branchConstraints.any { it.contains(token) }
    }
}
