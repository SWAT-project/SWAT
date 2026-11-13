// Loop-state matching in an instance method: the loop flips a field of the receiver. The state at
// the head includes the receiver (this, live) and the heap reachable from it, and main's frame,
// whose local c is live after the call. The states repeat, and the task is SAFE.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  int x = 0;

  void flip() {
    x = 1 - x;
  }

  void spin() {
    int v = 0;
    while (Verifier.nondetBoolean()) {
      v = Verifier.nondetInt();
      if (v > 10) {
        flip();
      }
    }
  }

  public static void main(String[] args) {
    Main c = new Main();
    c.spin();
    assert c.x == 0 || c.x == 1;
  }
}
