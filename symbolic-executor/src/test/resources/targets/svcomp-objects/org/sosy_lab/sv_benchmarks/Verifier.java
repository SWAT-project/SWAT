package org.sosy_lab.sv_benchmarks;

/** Minimal stand-in for the sv-benchmarks Verifier; the SV-COMP transformer redirects the calls. */
public final class Verifier {
  public static int nondetInt() {
    return 0;
  }

  public static <T> T nondetObject(Class<T> type, ObjectFactory<T> factory) {
    return factory.createObject();
  }
}
