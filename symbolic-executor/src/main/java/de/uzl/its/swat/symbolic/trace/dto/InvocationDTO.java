package de.uzl.its.swat.symbolic.trace.dto;

/**
 * Describes a single method invocation that could not be modelled symbolically during execution
 * (i.e. it returned a {@link de.uzl.its.swat.symbolic.value.PlaceHolder}). These are collected per
 * execution and shipped to the Symbolic Explorer as part of the trace so that the explorer owns the
 * consolidated per-testcase statistics instead of relying on log/stats-file scraping.
 *
 * <p>{@code contextLoss} marks the dangerous subset: invocations that received symbolic arguments
 * and therefore caused symbolic context loss (see {@code InvocationHandler}). Missing invocations
 * are the superset; context-loss invocations are the subset.
 */
public class InvocationDTO {
    @SuppressWarnings("unused")
    private String owner;

    @SuppressWarnings("unused")
    private String name;

    @SuppressWarnings("unused")
    private String desc;

    @SuppressWarnings("unused")
    private boolean isInstance;

    @SuppressWarnings("unused")
    private boolean isSymbolic;

    @SuppressWarnings("unused")
    private boolean contextLoss;

    @SuppressWarnings("unused")
    private int count;

    public InvocationDTO(
            String owner,
            String name,
            String desc,
            boolean isInstance,
            boolean isSymbolic,
            boolean contextLoss,
            int count) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.isInstance = isInstance;
        this.isSymbolic = isSymbolic;
        this.contextLoss = contextLoss;
        this.count = count;
    }

    /** Private default constructor for serialization */
    @SuppressWarnings("unused")
    private InvocationDTO() {}
}
