import org.sosy_lab.sv_benchmarks.Verifier;

/** Agent test target: the entry method calls another main method. */
public class Main {
  public static void main(String[] args) {
    Harness.main(new String[] {"a"});
    int y = Verifier.nondetInt();
    if (y == 2) {
      assert false : "nested target";
    }
  }
}

class Harness {
  static int calls = 0;

  public static void main(String[] args) {
    int z = Verifier.nondetInt();
    if (z > 1 && args.length == 1) {
      calls++;
    }
  }
}
