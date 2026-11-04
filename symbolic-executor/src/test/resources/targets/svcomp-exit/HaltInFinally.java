import org.sosy_lab.sv_benchmarks.Verifier;

/** Like ExitInFinally, with Runtime.halt (which skips the shutdown hooks). */
public class HaltInFinally {
  public static void main(String[] args) {
    int x = Verifier.nondetInt();
    try {
      assert x != 7;
    } finally {
      if (x == 7) Runtime.getRuntime().halt(3);
    }
  }
}
