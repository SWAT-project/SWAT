import org.sosy_lab.sv_benchmarks.Verifier;

/** Agent test target: the input is read by the static initializer of the entry class. */
public class Main {
  static int x = Verifier.nondetInt();

  public static void main(String[] args) {
    if (x > 5) {
      assert false : "clinit target";
    }
  }
}
