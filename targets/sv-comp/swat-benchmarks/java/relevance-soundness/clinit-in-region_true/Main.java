// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:2 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// The region is where a class is first used; its static initializer writes a field the assert's
// guard reads.
public class Main {
    static boolean initialized;

    static class Config {
        static int level;

        static {
            level = 3;
            Main.initialized = true;
        }
    }

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        for (int i = 0; i < 5; i++) {
            if (i == val) {
                System.out.println(Config.level);
            }
        }
        if (initialized) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
