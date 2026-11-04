// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// As lambda-static-field-noarg, but the lambda is created in main (invokedynamic in the graph).
public class Main {
    static int input;

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        input = a;
        Runnable check = () -> {
            if (input > 1000 && input < 1001) assert false;
        };
        if (b == 7) check.run();
        if (a > 3) a--;
    }
}
