// Hand-written facts: an irrelevant region covers what it calls. The loop between
// Swat.beginIrrelevant() and Swat.endIrrelevant() calls classify(), which branches three ways on
// each of 12 inputs (3^12 paths). The assertion reads a HashMap, so SWAT's relevance analysis
// cannot slice the logging. With the fact (--user-facts), classify's branches, called from the
// region, are no decisions.
import de.uzl.its.swat.annotations.Swat;
import java.util.HashMap;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static char classify(int v) {
    if (v > 1000) {
      return 'H';
    } else if (v < 0) {
      return 'N';
    }
    return '.';
  }

  public static void main(String[] args) {
    StringBuilder log = new StringBuilder();
    HashMap<String, Integer> config = new HashMap<>();
    config.put("limit", 100);
    Swat.beginIrrelevant();
    for (int i = 0; i < 12; i++) {
      log.append(classify(Verifier.nondetInt()));
    }
    Swat.endIrrelevant();
    int x = Verifier.nondetInt();
    if (x > config.get("limit") && x < 105) {
      assert false;
    }
  }
}
