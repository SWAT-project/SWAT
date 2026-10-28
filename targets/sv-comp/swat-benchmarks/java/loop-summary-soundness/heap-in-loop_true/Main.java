// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:4 int:1001
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.sv_benchmarks.Verifier;
// A counting loop that adds to a list, whose size the assert's guard reads.
public class Main {
    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        List<Integer> list = new ArrayList<Integer>();
        int i = 0;
        while (i < val) {
            i++;
            list.add(i);
        }
        if (list.size() == 4) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
