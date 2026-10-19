// As objarray-length_false, under no-runtime-exception: the violation is an IllegalStateException.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    int n = Verifier.nondetInt();
    if (n < 0 || n > 3) return;
    Object[] a = new Object[n]; // the shadow length stays symbolic
    n = 0; // n dead: only a.length carries the input
    // javac: a true condition falls through (outcome 0), so the then-side has the smaller position
    if (Verifier.nondetBoolean()) {
      if (a.length != 0) return; // the smaller path pins a.length == 0
    } else {
      if (Verifier.nondetBoolean()) { n = 0; } // same number of branches, a.length unconstrained
    }
    for (int i = 0; i < 2; i++) { // bounded: the baseline explores everything
      if (Verifier.nondetBoolean()) {
        if (a.length == 2) throw new IllegalStateException();
      }
    }
  }
}
