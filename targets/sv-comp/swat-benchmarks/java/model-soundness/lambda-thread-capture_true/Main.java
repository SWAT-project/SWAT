// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an integer x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// A capturing lambda runs in another thread (Thread.start, then join).
public class Main {
    public static void main(String[] args) throws InterruptedException {
        int x = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (b == 7) {
            Thread t = new Thread(() -> {
                if (x > 1000 && x < 1001) assert false;
            });
            t.start();
            t.join();
        }
    }
}
