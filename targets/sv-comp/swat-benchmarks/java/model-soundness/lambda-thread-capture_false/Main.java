// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// A capturing lambda runs in another thread (Thread.start, then join).
public class Main {
    public static void main(String[] args) throws InterruptedException {
        int x = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        if (b == 7) {
            Thread t = new Thread(() -> {
                if (x > 1000 && x < 100000) assert false;
            });
            t.start();
            t.join();
        }
    }
}
