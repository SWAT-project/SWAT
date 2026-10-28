package de.uzl.its.swat.common;

/**
 * Fresh copies of the String and boxed parameters of an entry method, so that symbolic execution
 * never works on interned or cached instances (see AbstractMethodAdapter.instrumentEntryPointParameters).
 *
 * <p>A parameter may be null (an optional request parameter or header). The null check is here
 * rather than inlined into the entry method: SWAT does not instrument this class, so it adds no
 * branch to the trace, which the static pre-analysis graph (built from the original bytecode)
 * would not have.
 */
public final class FreshCopy {
    private FreshCopy() {}

    public static String ofString(String s) {
        return s == null ? null : new String(s);
    }

    @SuppressWarnings("removal")
    public static Integer ofInteger(Integer v) {
        return v == null ? null : new Integer(v.intValue());
    }

    @SuppressWarnings("removal")
    public static Long ofLong(Long v) {
        return v == null ? null : new Long(v.longValue());
    }

    @SuppressWarnings("removal")
    public static Short ofShort(Short v) {
        return v == null ? null : new Short(v.shortValue());
    }

    @SuppressWarnings("removal")
    public static Byte ofByte(Byte v) {
        return v == null ? null : new Byte(v.byteValue());
    }

    @SuppressWarnings("removal")
    public static Character ofCharacter(Character v) {
        return v == null ? null : new Character(v.charValue());
    }

    @SuppressWarnings("removal")
    public static Boolean ofBoolean(Boolean v) {
        return v == null ? null : new Boolean(v.booleanValue());
    }
}
