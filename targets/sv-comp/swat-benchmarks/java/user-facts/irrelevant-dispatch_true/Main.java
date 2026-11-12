// Hand-written facts: @Irrelevant covers virtual calls and recursion below the annotated method.
// record() hands each input to one of two sinks (an interface call, chosen by the input), and
// one of them counts digits recursively; every step branches on the input. Only record() is
// annotated. The assertion reads a HashMap, so SWAT's relevance analysis cannot slice the
// logging. With the fact (--user-facts), all branches below record() are no decisions.
import de.uzl.its.swat.annotations.Irrelevant;
import java.util.HashMap;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static StringBuilder log = new StringBuilder();

  interface Sink {
    void put(int v);
  }

  static final class Short implements Sink {
    public void put(int v) {
      log.append(v < 0 ? 'N' : 'P');
    }
  }

  static final class Digits implements Sink {
    public void put(int v) {
      log.append(digits(v));
    }

    static int digits(int v) {
      if (v < 10) {
        return 1;
      }
      return 1 + digits(v / 10);
    }
  }

  @Irrelevant("only writes the log")
  static void record(int v) {
    Sink s = v > 500 ? new Digits() : new Short();
    s.put(v);
  }

  public static void main(String[] args) {
    HashMap<String, Integer> config = new HashMap<>();
    config.put("limit", 100);
    for (int i = 0; i < 8; i++) {
      record(Verifier.nondetInt());
    }
    int x = Verifier.nondetInt();
    if (x > config.get("limit") && x < 101) {
      assert false;
    }
  }
}
