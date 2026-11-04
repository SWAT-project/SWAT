import objects.Box;
import objects.BoxFactory;
import org.sosy_lab.sv_benchmarks.Verifier;

/** SV-COMP style target for nondetObject: the factory makes the choices with nondet calls. */
public class Main {
  public static void main(String[] args) {
    Box b = Verifier.nondetObject(Box.class, new BoxFactory());
    if (b != null && b.v == 42) {
      assert false : "nondet object";
    }
  }
}
