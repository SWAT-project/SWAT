// Loop-state matching: the loop keeps its counter in a System property, outside the hashed state.
// A run stops reporting states once it calls into the library.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    while (Verifier.nondetBoolean()) {
      int c = Integer.getInteger("loopstate.c", 0) + 1;
      System.setProperty("loopstate.c", Integer.toString(c));
      if (c == 3) assert false;
    }
  }
}
