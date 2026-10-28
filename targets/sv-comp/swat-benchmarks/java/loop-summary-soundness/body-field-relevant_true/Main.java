// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:5 int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// A counting loop whose body also counts in a field the assert's guard reads: the field's count
// depends on the trip count but has no closed form, so the summary must not be used.
public class Main {
    static int hits;

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        int i = 0;
        int s = 0;
        while (i < val) {
            i++;
            s++;
            hits = hits + 1;
        }
        if (hits == 5) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
