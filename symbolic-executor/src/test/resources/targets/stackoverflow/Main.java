import org.sosy_lab.sv_benchmarks.Verifier;

/** Recursion as deep as the first input; the second input selects whether main catches an overflow. */
public class Main {
    static int depth(int n) {
        if (n == 0) {
            return 0;
        }
        return 1 + depth(n - 1);
    }

    public static void main(String[] args) {
        int n = Verifier.nondetInt();
        boolean catchOverflow = Verifier.nondetInt() == 0;
        if (n < 0) {
            return;
        }
        if (!catchOverflow) {
            System.out.println("result " + depth(n));
            return;
        }
        int r;
        try {
            r = depth(n);
        } catch (StackOverflowError e) {
            r = -1;
        }
        System.out.println("result " + r);
        assert r == n;
    }
}
