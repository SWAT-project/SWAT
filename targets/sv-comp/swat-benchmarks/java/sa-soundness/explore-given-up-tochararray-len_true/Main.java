// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): string:abcdefg int:1001
import org.sosy_lab.sv_benchmarks.Verifier;
// toCharArray() pins the length of s (the array's length is concrete), and every run of a
// solution for the pin's other side diverges, so the explorer gives the pin up. A given-up branch
// rules out SAFE unless some run reaches its side; the branch's own solved marker counted as that
// (false SAFE before fix/given-up-self-coverage).
public class Main {
    public static void main(String[] args) {
        String s = Verifier.nondetString();
        int a = Verifier.nondetInt();
        char[] c = s.toCharArray();
        if (c.length == 7) {
            if (a > 1000 && a < 1001) assert false;
        }
    }
}
