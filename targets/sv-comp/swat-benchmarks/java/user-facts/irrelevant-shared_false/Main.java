// Soundness of @Irrelevant over callees: format() is called from the annotated record() and from
// main(). Only the calls from record() are covered; in main() the assertion depends on format's
// branch (x > 1000 gives "H"). A fact that covered format() everywhere would leave that branch
// unflipped and hide the violation.
import de.uzl.its.swat.annotations.Irrelevant;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static StringBuilder log = new StringBuilder();

  static String format(int v) {
    if (v > 1000) {
      return "H";
    }
    return v < 0 ? "N" : ".";
  }

  @Irrelevant("only writes the log")
  static void record(int v) {
    log.append(format(v));
  }

  public static void main(String[] args) {
    for (int i = 0; i < 4; i++) {
      record(Verifier.nondetInt());
    }
    String f = format(Verifier.nondetInt());
    if (f.equals("H")) {
      assert false;
    }
  }
}
