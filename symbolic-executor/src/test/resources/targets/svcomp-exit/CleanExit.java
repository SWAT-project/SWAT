import org.sosy_lab.sv_benchmarks.Verifier;

/** Ends the program with System.exit before an assertion that would fail. */
public class CleanExit {
  public static void main(String[] args) {
    int x = Verifier.nondetInt();
    if (x == 7) System.exit(0);
    assert x != 7;
  }
}
