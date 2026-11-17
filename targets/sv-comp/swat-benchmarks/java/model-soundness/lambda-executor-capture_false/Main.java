// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import java.util.concurrent.Executor;
import org.sosy_lab.sv_benchmarks.Verifier;
// A captured input is used inside a lambda that an Executor (itself a lambda) runs as a Runnable.
public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Executor executor = command -> command.run();
        if (b == 7) {
            executor.execute(() -> {
                if (x > 1000 && x < 100000) assert false;
            });
        }
    }
}
