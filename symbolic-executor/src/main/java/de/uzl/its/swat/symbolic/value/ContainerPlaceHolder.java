package de.uzl.its.swat.symbolic.value;

import de.uzl.its.swat.common.logging.records.InvocationEntry;
import de.uzl.its.swat.symbolic.value.reference.ContainerContents;
import java.util.List;

/**
 * The return value of a modelled call on a JDK container that returns a reference. The concrete
 * object is only known when the value is read back ({@code GETVALUE_Object}); the shadow is chosen
 * there, see {@code CollectionInvocation.resolve}.
 */
public class ContainerPlaceHolder extends PlaceHolder {

    public enum Result {
        /** An element (or map key/value) of {@link #contents}. */
        ELEMENT,
        /** A view, iterator, enumeration or entry backed by {@link #contents}. */
        VIEW,
        /** A new object array holding elements of {@link #contents} ({@code toArray()}). */
        ARRAY,
        /** A boolean or int the exact entries of {@link #contents} determine ({@link #expected}). */
        PRIMITIVE
    }

    public final Result result;
    public final ContainerContents contents;
    /** Whether a returned view yields map entries. */
    public final boolean yieldsEntries;
    /** Arguments the call may return as they are (e.g. the default of {@code getOrDefault}). */
    public final List<Value<?, ?>> candidates;
    /** The call, recorded as the culprit if the result cannot be matched to a shadow. */
    public final InvocationEntry call;
    /**
     * Whether the exact entries of a hash container say what the call returns: {@link #expected}
     * (for an element: its shadow, null for Java's null; for a primitive: a Boolean or Integer).
     * The concrete result is checked against it, see {@code CollectionInvocation}.
     */
    public final boolean exact;
    public final Object expected;
    /** Whether an exact result was decided by comparing symbolic keys (branches on the path). */
    public final boolean symbolic;

    public ContainerPlaceHolder(
            Result result,
            ContainerContents contents,
            boolean yieldsEntries,
            List<Value<?, ?>> candidates,
            InvocationEntry call) {
        this(result, contents, yieldsEntries, candidates, call, false, null, false);
    }

    private ContainerPlaceHolder(
            Result result,
            ContainerContents contents,
            boolean yieldsEntries,
            List<Value<?, ?>> candidates,
            InvocationEntry call,
            boolean exact,
            Object expected,
            boolean symbolic) {
        super(false);
        this.result = result;
        this.contents = contents;
        this.yieldsEntries = yieldsEntries;
        this.candidates = candidates;
        this.call = call;
        this.exact = exact;
        this.expected = expected;
        this.symbolic = symbolic;
    }

    /** The element (a shadow, or null for Java's null) the exact entries say the call returns. */
    public static ContainerPlaceHolder exactElement(ContainerContents contents, Value<?, ?> expected, InvocationEntry call) {
        return new ContainerPlaceHolder(Result.ELEMENT, contents, false, List.of(), call, true, expected, true);
    }

    /** The boolean or int the exact entries say the call returns. */
    public static ContainerPlaceHolder exactPrimitive(
            ContainerContents contents, Object expected, boolean symbolic, InvocationEntry call) {
        return new ContainerPlaceHolder(Result.PRIMITIVE, contents, false, List.of(), call, true, expected, symbolic);
    }

    @Override
    public String toString() {
        return "ContainerPlaceHolder (" + result + ")";
    }
}
