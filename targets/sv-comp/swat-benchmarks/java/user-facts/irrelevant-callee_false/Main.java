// Hand-written facts: @Irrelevant covers what the method calls. record() builds a log entry
// through a constructor and two helpers, each branching on the input (3^12 paths over 12 inputs);
// only record() is annotated. The assertion reads a HashMap, so SWAT's relevance analysis (all
// library state is one location) cannot slice the logging. With the fact (--user-facts), every
// branch below a call from record() is no decision: the executor tracks the fact on its call stack.
import de.uzl.its.swat.annotations.Irrelevant;
import java.util.HashMap;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static StringBuilder log = new StringBuilder();

  static final class Entry {
    final int level;

    Entry(int v) {
      if (v > 1000) {
        level = 2;
      } else if (v < 0) {
        level = 0;
      } else {
        level = 1;
      }
    }
  }

  static String format(Entry e) {
    if (e.level == 2) {
      return pad("H");
    }
    return e.level == 0 ? pad("N") : ".";
  }

  static String pad(String s) {
    return s.length() < 2 ? s + " " : s;
  }

  @Irrelevant("only writes the log")
  static void record(int v) {
    log.append(format(new Entry(v)));
  }

  public static void main(String[] args) {
    HashMap<String, Integer> config = new HashMap<>();
    config.put("limit", 100);
    for (int i = 0; i < 12; i++) {
      record(Verifier.nondetInt());
    }
    int x = Verifier.nondetInt();
    if (x > config.get("limit") && x < 105) {
      assert false;
    }
  }
}
