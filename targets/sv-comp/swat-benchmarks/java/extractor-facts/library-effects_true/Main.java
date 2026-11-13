// Hand-written library facts: the task of user-facts/irrelevant-method_true without @Irrelevant.
// record() branches three ways on each of 12 inputs (3^12 paths) but only appends to a log, and
// the assertion reads a HashMap. Without facts all library state is one location, so the log
// writes what the assertion reads and every branch is relevant. The stub file next to this class
// (swat-library-facts.txt) says that StringBuilder methods touch only StringBuilders and HashMap
// methods only HashMaps: then the relevance analysis itself slices record's branches. The facts
// are trusted; the verdict says it assumed them.
import java.util.HashMap;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static StringBuilder log = new StringBuilder();

  static void record(int v) {
    if (v > 1000) {
      log.append('H');
    } else if (v < 0) {
      log.append('N');
    } else {
      log.append('.');
    }
  }

  public static void main(String[] args) {
    HashMap<String, Integer> config = new HashMap<>();
    config.put("limit", 100);
    for (int i = 0; i < 12; i++) {
      record(Verifier.nondetInt());
    }
    int x = Verifier.nondetInt();
    if (x > config.get("limit") && x < 101) {
      assert false;
    }
  }
}
