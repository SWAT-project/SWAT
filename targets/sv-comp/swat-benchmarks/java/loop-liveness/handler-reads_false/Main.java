// Soundness of liveness at loop heads: last is overwritten in every iteration, but only after a
// division that may throw, and the handler reads it. So last is live at the head through the
// exception edge. One iteration with last = 42 (x stays 0) and a second with d = 0 reach the
// assertion; ignoring the exception edge would leave last out of the hash, match the state after
// the first iteration with the initial one, and hide the violation.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int flip(int x) {
    return 1 - x;
  }

  public static void main(String[] args) {
    int x = 0;
    int last = 0;
    try {
      while (Verifier.nondetBoolean()) {
        int d = Verifier.nondetInt();
        int q = 10 / d;
        last = Verifier.nondetInt() + q - q;
        if (last > 100) {
          x = flip(x);
        }
      }
    } catch (ArithmeticException e) {
      if (last == 42) {
        assert false;
      }
    }
  }
}
