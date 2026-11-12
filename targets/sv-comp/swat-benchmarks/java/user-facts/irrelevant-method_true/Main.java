// Hand-written facts: @Irrelevant on a method. record() branches three ways on each of 12 inputs
// (3^12 paths) but only appends to a log. The assertion reads a HashMap, and SWAT's relevance
// analysis treats all library state as one location, so it cannot tell that the log does not
// matter: without the fact the paths are explored one by one. With it (--user-facts), record's
// branches are no decisions. The fact is trusted; the verdict says it assumed it.
import de.uzl.its.swat.annotations.Irrelevant;
import java.util.HashMap;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static StringBuilder log = new StringBuilder();

  @Irrelevant("only writes the log")
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
