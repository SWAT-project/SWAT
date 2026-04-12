package de.uzl.its.swat.common;

import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.instrument.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

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
     * Conditionally uses Objects.equals() for de-interned classes,
     * otherwise uses reference equality.
     */
    @SuppressWarnings("unused")
    public static boolean refEquals(Object a, Object b) {
        if (Util.shouldUseValueEquality(a, b)) {
            return Objects.equals(a, b);
        }
        return a == b;
    }
}
