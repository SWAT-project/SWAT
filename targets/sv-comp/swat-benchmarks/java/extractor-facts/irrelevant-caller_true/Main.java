// An @Irrelevant fact that helps the graph beyond its own branches: verbose is set by 12 input
// branches in main and read only by log(), which is @Irrelevant. The executor makes log()'s
// branches no decisions, but main's branches stay decisions unless the relevance analysis also
// assumes the fact: then what log()'s branches read (verbose) is not relevant, and main's branches
// that only set it are sliced too. (log() appends to a StringBuilder and the assertion reads a
// HashMap, so the analysis cannot slice any of this without the fact. on() is a call so that the
// executor does not merge the if into a formula.)
import de.uzl.its.swat.annotations.Irrelevant;
import java.util.HashMap;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static StringBuilder out = new StringBuilder();

  @Irrelevant("only writes the log")
  static void log(boolean verbose) {
    if (verbose) {
      out.append('v');
    } else {
      out.append('.');
    }
  }

  static boolean on() {
    return true;
  }

  public static void main(String[] args) {
    HashMap<String, Integer> config = new HashMap<>();
    config.put("limit", 100);
    for (int i = 0; i < 12; i++) {
      boolean verbose = false;
      if (Verifier.nondetInt() > 5) {
        verbose = on();
      }
      log(verbose);
    }
    int x = Verifier.nondetInt();
    if (x > config.get("limit") && x < 101) {
      assert false;
    }
  }
}
