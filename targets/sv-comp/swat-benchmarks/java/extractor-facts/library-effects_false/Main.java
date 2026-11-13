// Soundness of hand-written library facts: the facts of library-effects_true, but the assertion
// reads the log (the StringBuilder family). record's branches decide what the log holds, so they
// stay relevant, and the violation (the fourth input above 1000) is found.
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
    if (log.charAt(3) == 'H') {
      assert false;
    }
  }
}
