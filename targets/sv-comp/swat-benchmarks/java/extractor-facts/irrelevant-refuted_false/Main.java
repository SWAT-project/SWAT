// A wrong @Irrelevant: check() contains the assertion, so its branch decides whether the target is
// reached. The extractor marks the method, and the relevance analysis refutes the fact (its code
// has a target point): the branch stays a decision, and the violation is found. Trusting the fact
// would leave the branch unflipped and answer SAFE.
import de.uzl.its.swat.annotations.Irrelevant;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  @Irrelevant("wrong: it holds the assertion")
  static void check(int v) {
    if (v == 4242) {
      assert false;
    }
  }

  public static void main(String[] args) {
    check(Verifier.nondetInt());
  }
}
