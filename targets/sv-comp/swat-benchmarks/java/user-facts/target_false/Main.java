// Hand-written facts: @Target. The program has no assertion; the property is that the shell sink
// exec() is never reached with the request's id (99). sanitize() clamps the id to 0..99.
// Without the fact (--user-facts) the task has no target point at all (the pre-check proves it
// assertion-free); with it, exec() starts with a failing assertion.
import de.uzl.its.swat.annotations.Target;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static String last = "";

  @Target("shell command with a request id")
  static void exec(String cmd) {
    last = cmd;
  }

  static int sanitize(int v) {
    if (v < 0) {
      return 0;
    }
    if (v > 99) {
      return 99;
    }
    return v;
  }

  public static void main(String[] args) {
    int id = sanitize(Verifier.nondetInt());
    int mode = Verifier.nondetInt();
    if (mode == 3 && id == 99) {
      exec("rm -rf /tmp/request-" + id);
    }
  }
}
