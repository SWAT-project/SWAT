import de.uzl.its.swat.annotations.Irrelevant;
import de.uzl.its.swat.annotations.Swat;
import de.uzl.its.swat.annotations.Target;
import org.sosy_lab.sv_benchmarks.Verifier;

/** Agent test target for hand-written facts: one of each kind. */
public class Main {
  @Irrelevant
  static int noise(int v) {
    return v > 7 ? 1 : helper(v);
  }

  /** Irrelevant when noise calls it, relevant when main does. */
  static int helper(int v) {
    return v > 2 ? 1 : 0;
  }

  @Target("sink")
  static void sink() {}

  public static void main(String[] args) {
    int a = Verifier.nondetInt();
    noise(a);
    helper(a + 1);
    Swat.beginIrrelevant();
    if (a > 3) {
      a = a + 0;
    }
    Swat.endIrrelevant();
    int s = 0;
    if (Swat.summary()) {
      s = 1;
    }
    if (a == 42) {
      sink();
    }
    System.out.println("summary " + s);
  }
}
