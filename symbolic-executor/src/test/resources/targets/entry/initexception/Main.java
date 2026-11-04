import org.sosy_lab.sv_benchmarks.Verifier;

/** Agent test target: an exception leaves a superclass constructor called by super(...). */
public class Main {
  public static void main(String[] args) {
    int v = Verifier.nondetInt();
    try {
      new Derived(v);
    } catch (IllegalStateException e) {
      assert false : "init target";
    }
  }
}

class Base {
  Base(int v) {
    if (v == 3) {
      throw new IllegalStateException("bad");
    }
  }
}

class Derived extends Base {
  Derived(int v) {
    super(v);
  }
}
