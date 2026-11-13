// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:2 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region prints an object whose toString() (called back by the library) counts.
public class Main {
    static int printed;

    static class Item {
        public String toString() {
            printed++;
            return "item";
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Item it = new Item();
        for (int i = 0; i < 5; i++) {
            if (i == val) {
                System.out.println(it);
            }
        }
        if (printed == 1) {
            if (b > 1000 && b < 100000) assert false;
        }
    }
}
