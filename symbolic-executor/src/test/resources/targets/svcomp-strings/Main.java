import org.sosy_lab.sv_benchmarks.Verifier;

/**
 * SV-COMP style target for the agent test of the index checks of String methods: charAt throws
 * for a nondet string shorter than 6 chars, and the exception is caught.
 */
public class Main {
  public static void main(String[] args) {
    String s = Verifier.nondetString();
    try {
      char c = s.charAt(5);
      assert c != 'x';
    } catch (StringIndexOutOfBoundsException e) {
      // too short
    }
  }
}
