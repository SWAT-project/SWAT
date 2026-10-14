package org.sosy_lab.sv_benchmarks;

/**
 * Replays chosen inputs, for checking task labels: -Dinputs="int:1001\nint:7\nstring:stop" (newline-separated) gives the
 * values in nondet call order; once they run out, every call returns 0 / false / "".
 */
public final class Verifier {
  private static final String[] INPUTS =
      System.getProperty("inputs", "").isEmpty() ? new String[0] : System.getProperty("inputs").split("\n");
  private static int next;

  private static String take(String type) {
    if (next >= INPUTS.length) return null;
    String[] tv = INPUTS[next++].split(":", 2);
    if (!tv[0].equals(type)) throw new IllegalStateException("input " + next + " is " + tv[0] + ", not " + type);
    return tv[1];
  }

  public static void assume(boolean condition) {
    if (!condition) Runtime.getRuntime().halt(1);
  }

  public static boolean nondetBoolean() { String v = take("boolean"); return v != null && Boolean.parseBoolean(v); }
  public static byte nondetByte() { String v = take("byte"); return v == null ? 0 : Byte.parseByte(v); }
  public static char nondetChar() { String v = take("char"); return v == null ? 0 : v.charAt(0); }
  public static short nondetShort() { String v = take("short"); return v == null ? 0 : Short.parseShort(v); }
  public static int nondetInt() { String v = take("int"); return v == null ? 0 : Integer.parseInt(v); }
  public static long nondetLong() { String v = take("long"); return v == null ? 0 : Long.parseLong(v); }
  public static float nondetFloat() { String v = take("float"); return v == null ? 0 : Float.parseFloat(v); }
  public static double nondetDouble() { String v = take("double"); return v == null ? 0 : Double.parseDouble(v); }
  public static String nondetString() { String v = take("string"); return v == null ? "" : v; }

  public static <T> T nondetObject(Class<T> type, ObjectFactory<T> factory) {
    return factory.createObject();
  }
}
