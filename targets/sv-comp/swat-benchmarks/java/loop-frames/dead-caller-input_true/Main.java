// Loop-state matching in a callee whose caller holds a symbolic local: v is an input that main
// reads before the call and never reads again, so it is dead after the call and left out of the
// caller's part of the state. mode (0 or 1, concrete on each path) is passed to spin and is live
// in spin. The states at spin's head repeat, and the task is SAFE. Requiring every local of the
// caller to be concrete would make all states unhashable and the run UNKNOWN.
// (flip is a call so that the executor does not merge the ifs into formulas.)
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int flip(int x) {
    return 1 - x;
  }

  static int spin(int mode) {
    int x = mode;
    int v = 0;
    while (Verifier.nondetBoolean()) {
      v = Verifier.nondetInt();
      if (v > 10) {
        x = flip(x);
      }
    }
    return x;
  }

  public static void main(String[] args) {
    int v = Verifier.nondetInt();
    int mode = 0;
    if (v > 100) {
      mode = flip(mode);
    }
    int r = spin(mode);
    assert r == 0 || r == 1;
  }
}
