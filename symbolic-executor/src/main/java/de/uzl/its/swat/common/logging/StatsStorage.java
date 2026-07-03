package de.uzl.its.swat.common.logging;

import de.uzl.its.swat.common.logging.records.InvocationEntry;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * In-memory accumulator for per-execution statistics: the methods that could not be modelled
 * symbolically ({@link #invocations}) and the subset of those that caused symbolic context loss
 * ({@link #contextLossInvocations}). This data is attached to the trace and sent to the Symbolic
 * Explorer (see {@code DTOBuilder}); the explorer owns the consolidated per-testcase output. The
 * executor itself no longer writes any stats file.
 */
@Getter
public class StatsStorage {
    private final HashMap<InvocationEntry, Integer> invocations;
    // Subset of invocations that received symbolic arguments and therefore caused context loss.
    private final Set<InvocationEntry> contextLossInvocations;

    public StatsStorage() {
        this.invocations = new HashMap<>();
        this.contextLossInvocations = new HashSet<>();
    }

    /** Marks an already-recorded missing invocation as a context-loss culprit. */
    public void recordContextLossInvocation(InvocationEntry entry) {
        contextLossInvocations.add(entry);
    }
}
