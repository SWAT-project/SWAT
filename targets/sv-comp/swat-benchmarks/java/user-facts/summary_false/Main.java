// Hand-written facts: a loop summary in plain Java (if (Swat.summary()) { closed form } else
// { loop }). The loop sums 0..n-1 for an input n <= 2000. The sum is quadratic in n, so SWAT's own
// loop summaries (affine) cannot merge it, and each n is its own path: n + 1 runs to cover them.
// With the summary (--user-facts) one path covers every n, with s = n(n-1)/2 as a formula.
// The summary is trusted; the verdict says it assumed it.
import de.uzl.its.swat.annotations.Swat;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    int n = Verifier.nondetInt();
    if (n < 0 || n > 2000) {
      return;
    }
    int s = 0;
    int i = 0;
    if (Swat.summary()) {
      s = n * (n - 1) / 2;
      i = n;
    } else {
      while (i < n) {
        s += i;
        i++;
      }
    }
    if (s > 5000 && n < 102) {
      assert false;
    }
  }
}
