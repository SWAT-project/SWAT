// Soundness: the same loop reached from the same call site, in twice, with different values of
// twice's live local a. Hashing the call site but not the caller's live locals would make the
// states of the call with a = 1 repeats of those with a = 0 and miss the violation (a false SAFE).
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

  static int twice(int a) {
    int b = spin();
    return a + b;
  }

  public static void main(String[] args) {
    int a = spin();
    int s = twice(a);
    assert s != 2;
  }
}
