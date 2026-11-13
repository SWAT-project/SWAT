// Loop-state matching with several dead locals at the outer head: the input v and the inner
// loop's count k are both written again before any read in the next iteration, so at the outer
// head only x is live and the states repeat. (At the inner head v is live, read after the inner
// loop, so states there stay unhashable; the inner loop ends after 3 steps anyway.) Hashing the
// dead input (symbolic) at the outer head would make every state unhashable and the run UNKNOWN.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int flip(int x) {
    return 1 - x;
  }

  public static void main(String[] args) {
    int x = 0;
    int v = 0;
    int k = 0;
    while (Verifier.nondetBoolean()) {
      v = Verifier.nondetInt();
      k = 0;
      while (k < 3 && Verifier.nondetBoolean()) {
        k = k + 1;
      }
      if (v > 10 && k == 2) {
        x = flip(x);
      }
    }
    assert x == 0 || x == 1;
  }
}
