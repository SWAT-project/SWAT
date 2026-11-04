import org.sosy_lab.sv_benchmarks.Verifier;

/** The assertion fails, then the finally ends the program before the AssertionError escapes. */
public class ExitInFinally {
  public static void main(String[] args) {
    int x = Verifier.nondetInt();
    try {
      assert x != 7;
    } finally {
      if (x == 7) System.exit(3);
    }
  }
}
