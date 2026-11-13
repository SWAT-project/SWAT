// Loop-state matching two calls deep: main calls run, run calls spin, and the loop is in spin.
// The state at spin's head includes both callers: run's call site with its live local base, and
// main's call site. The states repeat, and the task is SAFE.
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

  static int run(int base) {
    int r = spin();
    return base + r;
  }

  public static void main(String[] args) {
    int s = run(5);
    assert s == 5 || s == 6;
  }
}
