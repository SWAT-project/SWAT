// Hand-written fact @NotCalledBack: Main.toString() asserts, and overrides a library method, so the
// extractor must assume that library code may call it back: every library call becomes a target
// point, and each of the 12 logging branches (each ends in a library call) is relevant: 2^12
// paths. Library code never calls it here (main calls it directly, after the logging), so the fact
// holds; with it the logging branches are sliced. The fact is checked on every run (a callback
// into it refutes it) and assumed by the verdict.
import de.uzl.its.swat.annotations.NotCalledBack;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  int id;

  Main(int id) {
    this.id = id;
  }

  @NotCalledBack("only main calls it")
  @Override
  public String toString() {
    assert id >= 0;
    return "item";
  }

  public static void main(String[] args) {
    StringBuilder log = new StringBuilder();
    for (int i = 0; i < 12; i++) {
      if (Verifier.nondetInt() > 0) {
        log.append('+');
      }
    }
    Main item = new Main(Math.abs(Verifier.nondetInt() % 1000));
    item.toString();
  }
}
