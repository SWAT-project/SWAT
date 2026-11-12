// Loop-state matching with a dead local: v holds a fresh input in every iteration and is written
// again before any read, so it is dead at the loop head. x only alternates between 0 and 1, so the
// states at the head repeat. Hashing v (symbolic) would make every state unhashable and the run
// UNKNOWN; hashing only live locals lets the states match, and the task is SAFE.
// (flip is a call so that the executor does not merge the if into a formula, which would make x
// symbolic.)
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int flip(int x) {
    return 1 - x;
  }

  public static void main(String[] args) {
    int x = 0;
    int v = 0;
    while (Verifier.nondetBoolean()) {
      v = Verifier.nondetInt();
      if (v > 10) {
        x = flip(x);
      }
    }
    assert x == 0 || x == 1;
  }
}
