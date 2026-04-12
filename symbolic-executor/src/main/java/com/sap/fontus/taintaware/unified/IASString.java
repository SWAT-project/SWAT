package com.sap.fontus.taintaware.unified;

/**
 * Stub version of IASString.  This class is only here so that:
 *   1) SWAT’s ASM instrumentation can compile against a known IASString type.
 *   2) At runtime, if the real Fontus JAR is NOT present (or is placed after SWAT
 *      in the classpath), this stub will be used, and its behavior will simply
 *      wrap a raw String in a wrapper that delegates toString() back to the raw.
 *
 * If the real Fontus JAR is placed *before* SWAT on the classpath, the JVM will
 * load the real IASString instead, because class loading always picks the FIRST
 * matching FQCN in the classpath.
 *
 * IMPORTANT: Make sure this stub’s package+class name exactly matches the real
 * Fontus class as shipped by SAP: com.sap.fontus.taintaware.unified.IASString
 * and that it provides at least these two methods:
 *   • public static IASString valueOf(String)
 *   • public String toString()
 * so that LiftAdapter (and any other code that calls
 * IASString.valueOf(...) or .toString()) will still compile and execute.
 */
public class IASString {
    private final String string;

    private IASString(String s) {
        this.string = s;
    }

    /**
     * Mimic the real Fontus signature.  The real IASString.valueOf(...)
     * probably does more taint‐tracking under the hood.  Our stub version
     * simply wraps the raw String.
     */
    public static IASString valueOf(String s) {
        return new IASString(s);
    }

    public String getString() {
        return this.string;
    }

    // You can add any other no-op methods here to match the real API,
    // but these two are the bare minimum so that your LiftAdapter can
    // compile against IASString.valueOf(...) and still print/log the
    // current contents.
}
