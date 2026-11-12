// Hand-written facts: an irrelevant region inside a method (Swat.beginIrrelevant/endIrrelevant
// around a loop). The loop branches three ways on each of 12 inputs (3^12 paths) but only appends
// to a log. The assertion reads a HashMap, and SWAT's relevance analysis treats all library state
// as one location, so it cannot tell that the log does not matter. With the fact (--user-facts),
// the region's branches are no decisions. The fact is trusted; the verdict says it assumed it.
import de.uzl.its.swat.annotations.Swat;
import java.util.HashMap;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    StringBuilder log = new StringBuilder();
    HashMap<String, Integer> config = new HashMap<>();
    config.put("limit", 100);
    Swat.beginIrrelevant();
    for (int i = 0; i < 12; i++) {
      int v = Verifier.nondetInt();
      if (v > 1000) {
        log.append('H');
      } else if (v < 0) {
        log.append('N');
      } else {
        log.append('.');
      }
    }
    Swat.endIrrelevant();
    int x = Verifier.nondetInt();
    if (x > config.get("limit") && x < 105) {
      assert false;
    }
  }
}
