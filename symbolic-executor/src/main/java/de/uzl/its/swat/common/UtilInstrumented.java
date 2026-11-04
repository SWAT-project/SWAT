package de.uzl.its.swat.common;

import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.instrument.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class UtilInstrumented {

    @SuppressWarnings("unused")
    public static void liftClass(Object param, String paramCnameDot, String methodName, String symbolicPrefix) {
        try {
            Class<?> c  = Class.forName(paramCnameDot, true, Thread.currentThread().getContextClassLoader());
            Method m = c.getDeclaredMethod(methodName, String.class);

            m.invoke(param, symbolicPrefix);
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            GlobalLogger.getSymbolicExecutionLogger().warn("Could not call " + methodName + " on "
                    + paramCnameDot + ".");
            e.printStackTrace();
        }
    }
    /**
     * Replaces {@code IF_ACMPEQ}/{@code IF_ACMPNE}: Java reference identity, computed on objects of
     * which SWAT de-interned the literals and boxing caches (see {@link RefIdentity}).
     *
     * <p>Two canonical instances (literals, interned strings, cached boxes) are identical exactly
     * when their values are equal, and that comparison goes through the modelled {@code equals}
     * and unboxing calls so the symbolic state sees its dependence on the values. Every other
     * comparison is plain identity and concrete. The comparisons themselves must not be
     * {@code IF_ACMP}s here: the symbolic state encodes those on strings as value equality.
     */
    @SuppressWarnings("unused")
    public static boolean refEquals(Object a, Object b) {
        if (a instanceof String sa && b instanceof String sb) {
            if (RefIdentity.isCanonicalString(sa) && RefIdentity.isCanonicalString(sb)) {
                if (RefIdentity.isPooledOnly(sa) || RefIdentity.isPooledOnly(sb)) {
                    // Whether a string from uninstrumented code is pooled may depend on its value.
                    RefIdentity.valueDependentIdentity(sa, sb);
                }
                return sa.equals(sb);
            }
            return RefIdentity.same(a, b);
        }
        if (RefIdentity.same(a, b)) {
            return true;
        }
        if (RefIdentity.isCanonicalBox(a) && RefIdentity.isCanonicalBox(b)) {
            // Distinct cached boxes: the same instance iff the values are equal and in the cache range.
            if (a instanceof Integer x && b instanceof Integer y) {
                int v = x.intValue();
                return v == y.intValue() && v >= -128 && v <= 127;
            }
            if (a instanceof Long x && b instanceof Long y) {
                long v = x.longValue();
                return v == y.longValue() && v >= -128 && v <= 127;
            }
            if (a instanceof Short x && b instanceof Short y) {
                short v = x.shortValue();
                return v == y.shortValue() && v >= -128 && v <= 127;
            }
            if (a instanceof Character x && b instanceof Character y) {
                char v = x.charValue();
                return v == y.charValue() && v <= 127;
            }
            if (a instanceof Byte x && b instanceof Byte y) {
                return x.byteValue() == y.byteValue();
            }
            if (a instanceof Boolean x && b instanceof Boolean y) {
                return x.booleanValue() == y.booleanValue();
            }
        }
        return RefIdentity.same(a, b);
    }

    /**
     * Replaces {@code String.intern()}: a de-interned copy standing for the pooled instance, or
     * {@code s} itself when {@code s} is (or, by this call, becomes) the pooled instance.
     */
    @SuppressWarnings("unused")
    public static String intern(String s) {
        if (RefIdentity.internsToItself(s)) {
            return s;
        }
        String copy = new String(s);
        RefIdentity.markDeInterned(copy);
        return copy;
    }
}
