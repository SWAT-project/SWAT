import org.sosy_lab.sv_benchmarks.Verifier;

/**
 * SV-COMP style target for the agent test of the witness data in the trace: nondet calls of
 * several types, two of them on one line, and an assertion that fails in a helper method.
 */
public class Main {
  static int check(int a, int b) {
    assert a + b != 7 : "witness target";
    return a + b;
  }

  public static void main(String[] args) {
    int a = Verifier.nondetInt(), b = Verifier.nondetInt();
    double d = Verifier.nondetDouble();
    String s = Verifier.nondetString();
    if (d < 0.5 && s.length() > 3) {
      check(a, b);
    }
  }
}
