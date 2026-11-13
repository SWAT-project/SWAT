// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:2 int:1001
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.sosy_lab.sv_benchmarks.Verifier;
// The region calls library code that calls back into a comparator, which writes a field the
// assert's guard reads.
public class Main {
    static boolean compared;

    public static void main(String[] args) {
        int val = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        List<Integer> list = new ArrayList<Integer>();
        list.add(2);
        list.add(1);
        for (int i = 0; i < 5; i++) {
            if (i == val) {
                Collections.sort(list, new Comparator<Integer>() {
                    public int compare(Integer x, Integer y) {
                        compared = true;
                        return x - y;
                    }
                });
            }
        }
        if (compared) {
            if (b > 1000 && b < 1001) assert false;
        }
    }
}
