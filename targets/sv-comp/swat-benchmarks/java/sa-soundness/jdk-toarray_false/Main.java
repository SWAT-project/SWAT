// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.sv_benchmarks.Verifier;
// An array returned by an uninstrumented JDK call is read before the assert (bounds guard on an
// untracked array; regression test for the phantom bounds-guard bug).
public class Main {
    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        List<Integer> list = new ArrayList<Integer>();
        list.add(3);
        list.add(5);
        Object[] arr = list.toArray();
        int x = (Integer) arr[1];
        if (b == 7) {
            if (a + x - 5 > 1000 && a + x - 5 < 100000) assert false;
        }
    }
}
