package de.uzl.its.swat.staticcheck;

import java.util.Objects;
import org.objectweb.asm.tree.analysis.Value;

/**
 * Abstract value of a local variable or operand stack slot for {@link ExceptionFreedomCheck}.
 *
 * <p>Every fact is an over-approximation of the concrete values the slot can hold: {@code BOTTOM}
 * holds none (not reached yet), {@code NULL} only null, {@code NONNULL} only references, {@code REF}
 * references or null, {@code PRIM} a primitive and {@code UNKNOWN} anything. The optional facts
 * narrow this further and are dropped (set to null) whenever two values with different facts meet.
 */
final class AbsValue implements Value {

    enum Kind {
        BOTTOM,
        PRIM,
        NULL,
        NONNULL,
        REF,
        UNKNOWN
    }

    final Kind kind;
    final int size;
    /** PRIM: the int or long value, if it is a known constant. */
    final Long constant;
    /** NONNULL array: its length, if it is a known constant. */
    final Integer length;
    /**
     * NONNULL array: its length equals the number of constants of this enum class. PRIM: the value
     * is that number.
     */
    final String lengthOf;
    /** PRIM: the value is the ordinal of a constant of this enum class (0 <= v < its constant count). */
    final String ordinalOf;
    /** NONNULL: the runtime class of the object (internal name, or descriptor for arrays). */
    final String exactType;
    /** NULL/NONNULL/REF: a type every non-null value is an instance of. */
    final String staticType;
    /** The receiver of the current instance method, unchanged since method entry. */
    final boolean isThis;
    /** The slot may hold the receiver of the current instance method (a may-alias fact, kept by joins). */
    final boolean mayBeThis;
    /**
     * The exception caught by the handler with this index in the method's try-catch list (-1: not a
     * caught exception, or not known to be one).
     */
    final int caughtBy;

    static final AbsValue UNKNOWN1 = new AbsValue(Kind.UNKNOWN, 1, null, null, null, null, null, null, false);
    static final AbsValue PRIM1 = prim(1);
    static final AbsValue PRIM2 = prim(2);
    static final AbsValue NULL = new AbsValue(Kind.NULL, 1, null, null, null, null, null, null, false);

    AbsValue(Kind kind, int size, Long constant, Integer length, String lengthOf, String ordinalOf,
             String exactType, String staticType, boolean isThis) {
        this(kind, size, constant, length, lengthOf, ordinalOf, exactType, staticType, isThis, -1);
    }

    AbsValue(Kind kind, int size, Long constant, Integer length, String lengthOf, String ordinalOf,
             String exactType, String staticType, boolean isThis, int caughtBy) {
        this(kind, size, constant, length, lengthOf, ordinalOf, exactType, staticType, isThis, caughtBy, isThis);
    }

    private AbsValue(Kind kind, int size, Long constant, Integer length, String lengthOf, String ordinalOf,
                     String exactType, String staticType, boolean isThis, int caughtBy, boolean mayBeThis) {
        this.mayBeThis = mayBeThis || isThis;
        this.kind = kind;
        this.size = size;
        this.constant = constant;
        this.length = length;
        this.lengthOf = lengthOf;
        this.ordinalOf = ordinalOf;
        this.exactType = exactType;
        this.staticType = staticType;
        this.isThis = isThis;
        this.caughtBy = caughtBy;
    }

    static AbsValue prim(int size) {
        return new AbsValue(Kind.PRIM, size, null, null, null, null, null, null, false);
    }

    static AbsValue constant(int size, long value) {
        return new AbsValue(Kind.PRIM, size, value, null, null, null, null, null, false);
    }

    static AbsValue bottom(int size) {
        return new AbsValue(Kind.BOTTOM, size, null, null, null, null, null, null, false);
    }

    static AbsValue ref(String staticType) {
        return new AbsValue(Kind.REF, 1, null, null, null, null, null, staticType, false);
    }

    static AbsValue nonNull(String staticType) {
        return new AbsValue(Kind.NONNULL, 1, null, null, null, null, null, staticType, false);
    }

    /** A non-null object whose runtime class is known (a fresh allocation, a String literal, ...). */
    static AbsValue exact(String type) {
        return new AbsValue(Kind.NONNULL, 1, null, null, null, null, type, type, false);
    }

    static AbsValue array(String type, Integer length, String lengthOf) {
        return new AbsValue(Kind.NONNULL, 1, null, length, lengthOf, null, type, type, false);
    }

    AbsValue withThis() {
        return new AbsValue(kind, size, constant, length, lengthOf, ordinalOf, exactType, staticType, true);
    }

    static AbsValue caught(String type, int handler) {
        return new AbsValue(Kind.NONNULL, 1, null, null, null, null, null, type, false, handler);
    }

    /** The same value without the facts that only hold where it was computed (receiver, handler). */
    AbsValue escaped() {
        if (!mayBeThis && caughtBy < 0) return this;
        return new AbsValue(kind, size, constant, length, lengthOf, ordinalOf, exactType, staticType, false, -1);
    }

    AbsValue withOrdinalOf(String enumType) {
        return new AbsValue(kind, size, constant, length, lengthOf, enumType, exactType, staticType, isThis, caughtBy, mayBeThis);
    }

    AbsValue withLengthOf(String enumType) {
        return new AbsValue(kind, size, constant, length, enumType, ordinalOf, exactType, staticType, isThis, caughtBy, mayBeThis);
    }

    boolean isBottom() {
        return kind == Kind.BOTTOM;
    }

    boolean isNull() {
        return kind == Kind.NULL;
    }

    /** No concrete value of this slot can be null (BOTTOM holds no value at all). */
    boolean isNonNull() {
        return kind == Kind.NONNULL || kind == Kind.BOTTOM;
    }

    boolean isReference() {
        return kind == Kind.NULL || kind == Kind.NONNULL || kind == Kind.REF;
    }

    @Override
    public int getSize() {
        return size;
    }

    /** The least upper bound of two values. */
    static AbsValue join(AbsValue a, AbsValue b) {
        AbsValue j = joinFacts(a, b);
        if ((a.mayBeThis || b.mayBeThis) && !j.mayBeThis) {
            j = new AbsValue(j.kind, j.size, j.constant, j.length, j.lengthOf, j.ordinalOf, j.exactType, j.staticType,
                    j.isThis, j.caughtBy, true);
        }
        return j;
    }

    private static AbsValue joinFacts(AbsValue a, AbsValue b) {
        if (a.equals(b)) return a;
        if (a.isBottom()) return b;
        if (b.isBottom()) return a;
        if (a.size != b.size) return UNKNOWN1;
        if (a.kind == Kind.PRIM && b.kind == Kind.PRIM) {
            return new AbsValue(Kind.PRIM, a.size, same(a.constant, b.constant), null,
                    same(a.lengthOf, b.lengthOf), same(a.ordinalOf, b.ordinalOf), null, null, false);
        }
        if (a.isReference() && b.isReference()) {
            boolean thisRef = a.isThis && b.isThis;
            if (a.kind == Kind.NULL || b.kind == Kind.NULL) {
                AbsValue other = a.kind == Kind.NULL ? b : a;
                return new AbsValue(other.kind == Kind.NULL ? Kind.NULL : Kind.REF, 1, null, null, null, null,
                        null, other.staticType, false);
            }
            if (a.kind == Kind.NONNULL && b.kind == Kind.NONNULL) {
                return new AbsValue(Kind.NONNULL, 1, null, same(a.length, b.length), same(a.lengthOf, b.lengthOf),
                        null, same(a.exactType, b.exactType), same(a.staticType, b.staticType), thisRef,
                        a.caughtBy == b.caughtBy ? a.caughtBy : -1);
            }
            return new AbsValue(Kind.REF, 1, null, null, null, null, null, same(a.staticType, b.staticType), false);
        }
        return a.size == 1 ? UNKNOWN1 : new AbsValue(Kind.UNKNOWN, a.size, null, null, null, null, null, null, false);
    }

    private static <T> T same(T a, T b) {
        return Objects.equals(a, b) ? a : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbsValue)) return false;
        AbsValue v = (AbsValue) o;
        return kind == v.kind && size == v.size && isThis == v.isThis && mayBeThis == v.mayBeThis && caughtBy == v.caughtBy
                && Objects.equals(constant, v.constant)
                && Objects.equals(length, v.length) && Objects.equals(lengthOf, v.lengthOf)
                && Objects.equals(ordinalOf, v.ordinalOf) && Objects.equals(exactType, v.exactType)
                && Objects.equals(staticType, v.staticType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, size, constant, length, lengthOf, ordinalOf, exactType, staticType, isThis, mayBeThis, caughtBy);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(kind.name());
        if (constant != null) sb.append('=').append(constant);
        if (length != null) sb.append(" len=").append(length);
        if (lengthOf != null) sb.append(" lenOf=").append(lengthOf);
        if (ordinalOf != null) sb.append(" ordOf=").append(ordinalOf);
        if (exactType != null) sb.append(" exact=").append(exactType);
        else if (staticType != null) sb.append(" type=").append(staticType);
        if (isThis) sb.append(" this");
        else if (mayBeThis) sb.append(" maybe-this");
        if (caughtBy >= 0) sb.append(" caught#").append(caughtBy);
        return sb.toString();
    }
}
