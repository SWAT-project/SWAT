// An @Irrelevant fact the relevance analysis can check: count()'s branches only change a counter
// that nothing reads. The analysis slices them by itself, so the fact is not needed: the SAFE
// verdict assumes no user fact (stats.json: user_facts.assumed is 0).
import de.uzl.its.swat.annotations.Irrelevant;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int hits = 0;

  @Irrelevant("only counts")
  static void count(int v) {
    if (v > 1000) {
      hits++;
    } else if (v < 0) {
      hits--;
    }
  }

  public static void main(String[] args) {
    for (int i = 0; i < 12; i++) {
      count(Verifier.nondetInt());
    }
    int x = Verifier.nondetInt();
    if (x > 100 && x < 101) {
      assert false;
    }
  }
}
