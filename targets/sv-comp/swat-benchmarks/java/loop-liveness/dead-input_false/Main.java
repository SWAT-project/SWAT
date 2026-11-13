// The loop of dead-input_true with a reachable assertion: x is 1 after one iteration with v > 10.
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
    assert x == 0;
  }
}
