// Loop-state matching in a callee: the loop of loop-liveness/dead-input_true, moved into a method
// that main calls. The head's frame is not the only application frame, so the state at the head
// includes main's frame: the call site main continues at and main's locals live after the call
// (none here). x only alternates between 0 and 1, so the states repeat and the task is SAFE.
// Before states could span frames, every state in spin was unhashable and the run UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int flip(int x) {
    return 1 - x;
  }

  static int spin() {
    int x = 0;
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
    int r = spin();
    assert r == 0 || r == 1;
  }
}
