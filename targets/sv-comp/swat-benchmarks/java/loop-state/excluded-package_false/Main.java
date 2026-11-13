// Loop-state matching: Counter lives in a package SWAT does not instrument (com/google), so its
// static field is outside the hashed state.
import org.sosy_lab.sv_benchmarks.Verifier;
import com.google.loopstate.Counter;

public class Main {
  public static void main(String[] args) {
    while (Verifier.nondetBoolean()) {
      if (Counter.inc() == 3) assert false;
    }
  }
}
