// Soundness of liveness at loop heads: v is read after the loop, so it is live at the head. After
// one iteration with v = 42 (<= 10 keeps x at 0) the state differs from the initial one only in v;
// leaving v out of the hash would match the two and hide the violation (one iteration, then exit).
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
      if (v > 100) {
        x = flip(x);
      }
    }
    if (v == 42) {
      assert false;
    }
  }
}
