// Loop-state matching: the thread interrupt flag is state outside the hash. Two paths reach the
// same hashed state with the flag clear and set; Thread.interrupted() later tells them apart.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    if (Verifier.nondetBoolean()) {
      if (Verifier.nondetBoolean()) { args = args; } // smaller position: flag clear
    } else {
      Thread.currentThread().interrupt(); // larger position: flag set, same hashed state
      if (Verifier.nondetBoolean()) { args = args; }
    }
    for (int i = 0; i < 2; i++) {
      if (Verifier.nondetBoolean()) {
        if (Thread.interrupted()) assert false;
      }
    }
  }
}
