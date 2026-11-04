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
        ARRAY
    }

    public final Result result;
    public final ContainerContents contents;
    /** Whether a returned view yields map entries. */
    public final boolean yieldsEntries;
    /** Arguments the call may return as they are (e.g. the default of {@code getOrDefault}). */
    public final List<Value<?, ?>> candidates;
    /** The call, recorded as the culprit if the result cannot be matched to a shadow. */
    public final InvocationEntry call;

    public ContainerPlaceHolder(
            Result result,
            ContainerContents contents,
            boolean yieldsEntries,
            List<Value<?, ?>> candidates,
            InvocationEntry call) {
        super(false);
        this.result = result;
        this.contents = contents;
        this.yieldsEntries = yieldsEntries;
        this.candidates = candidates;
        this.call = call;
    }

    @Override
    public String toString() {
        return "ContainerPlaceHolder (" + result + ")";
    }
}
