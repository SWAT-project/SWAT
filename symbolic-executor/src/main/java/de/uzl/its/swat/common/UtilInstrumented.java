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
     * Models reference equality ({@code ==}) for de-interned value types by comparing the ORIGINAL
     * identities. De-interning gave {@code a}/{@code b} fresh identities that diverge from the
     * real JVM; comparing {@link Provenance#root}s (the canonical object each was de-interned from)
     * reproduces the un-transformed program's {@code ==}: two copies of the same interned literal /
     * cached box / returned object share one canonical and so compare equal. Non-de-interned classes
     * keep plain reference equality.
     */
    @SuppressWarnings("unused")
    public static boolean refEquals(Object a, Object b) {
        if (Util.shouldUseValueEquality(a, b)) {
            return Provenance.root(a) == Provenance.root(b);
        }
        return a == b;
    }
}
