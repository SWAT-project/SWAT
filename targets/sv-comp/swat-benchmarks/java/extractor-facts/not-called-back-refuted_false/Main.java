// A wrong @NotCalledBack: java.util.Objects.toString(Object) calls Main.toString() back on the
// first run already. The executor refutes the fact there, so the verdict cannot be SAFE, and the
// violation inside the callback (a negative id) is found.
import de.uzl.its.swat.annotations.NotCalledBack;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  int id;

  Main(int id) {
    this.id = id;
  }

  @NotCalledBack("wrong: Objects.toString calls it")
  @Override
  public String toString() {
    assert id >= 0;
    return "item";
  }

  public static void main(String[] args) {
    Main item = new Main(Verifier.nondetInt());
    java.util.Objects.toString(item);
  }
}
