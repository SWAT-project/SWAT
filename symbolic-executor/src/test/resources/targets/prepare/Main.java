import org.sosy_lab.sv_benchmarks.Verifier;

/**
 * Agent test target for prepared classes: every rewrite applies (string literals, boxing,
 * reference comparisons, nondet calls), and the method has a loop and a diamond.
 */
public class Main {
  static Integer box(int v) {
    return v;
  }

  public static void main(String[] args) {
    int n = Verifier.nondetInt();
    String s = "lit";
    Object a = box(n);
    Object b = Verifier.nondetBoolean() ? a : box(n);
    int sum = 0;
    for (int i = 0; i < 3; i++) {
      sum += (n > i) ? 2 : 1;
    }
    if (a == b && s.length() == 3 && sum == 6) {
      assert n < 5 : "prepared";
    }
  }
}
