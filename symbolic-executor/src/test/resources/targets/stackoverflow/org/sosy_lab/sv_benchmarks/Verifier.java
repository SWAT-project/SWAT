package org.sosy_lab.sv_benchmarks;

/** Minimal stand-in for the sv-benchmarks Verifier; the SV-COMP transformer redirects the calls. */
public final class Verifier {
  public static void assume(boolean condition) {
    if (!condition) {
      Runtime.getRuntime().halt(1);
    }
  }

  public static boolean nondetBoolean() {
    return false;
  }

  public static byte nondetByte() {
    return 0;
  }

  public static char nondetChar() {
    return 0;
  }

  public static short nondetShort() {
    return 0;
  }

  public static int nondetInt() {
    return 0;
  }

  public static long nondetLong() {
    return 0;
  }

  public static float nondetFloat() {
    return 0;
  }

  public static double nondetDouble() {
    return 0;
  }

  public static String nondetString() {
    return "";
  }
}
