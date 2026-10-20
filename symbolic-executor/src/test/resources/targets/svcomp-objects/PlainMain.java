import objects.Box;
import org.sosy_lab.fixtures.PlainBoxFactory;
import org.sosy_lab.sv_benchmarks.Verifier;

/** nondetObject with a factory that is not instrumented. */
public class PlainMain {
  public static void main(String[] args) {
    Box b = Verifier.nondetObject(Box.class, new PlainBoxFactory());
    if (b != null) {
      assert false : "nondet object";
    }
  }
}
