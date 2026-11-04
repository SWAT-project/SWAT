import org.sosy_lab.sv_benchmarks.Verifier;

/**
 * SV-COMP style target for the agent test of reference comparisons: SWAT rewrites a == b into a
 * call to UtilInstrumented.refEquals, whose branches the static pre-analysis graph does not have.
 */
public class Main {
  public static void main(String[] args) {
    Object a = new Object();
    Object b = Verifier.nondetBoolean() ? a : new Object();
    if (a == b) {
      assert b != null;
    }
  }
}
