// Soundness: the same loop reached from two call sites, with the same callee state and no live
// locals in main. The states at spin's head in the second call equal those in the first except
// for where main continues. Hashing only spin's frame would make the second call's states repeats
// of the first's, leave its branches unflipped, and miss the violation (a false SAFE): the call
// site must be part of the state.
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int flip(int x) {
    return 1 - x;
  }

  static int spin() {
    int x = 0;
    int v = 0;
    while (Verifier.nondetBoolean()) {
      v = Verifier.nondetInt();
      if (v > 10) {
        x = flip(x);
      }
    }
    return x;
  }

  public static void main(String[] args) {
    if (spin() == 1) {
      if (spin() == 1) {
        assert false;
      }
    }
  }
}
