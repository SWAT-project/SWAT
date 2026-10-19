// As objarray-length_false, with an unbounded loop: a wrong match gives SAFE where the search can
// never finish otherwise.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    int n = Verifier.nondetInt();
    if (n < 0 || n > 3) return;
    Object[] a = new Object[n];
    n = 0; // n dead: only a.length carries the input
    // javac: a true condition falls through (outcome 0), so the then-side has the smaller position
    if (Verifier.nondetBoolean()) {
      if (a.length != 0) return; // the smaller path pins a.length == 0
    } else {
      if (Verifier.nondetBoolean()) { n = 0; } // same number of branches, a.length unconstrained
    }
    while (Verifier.nondetBoolean()) {
      if (a.length == 2) assert false;
    }
  }
}
