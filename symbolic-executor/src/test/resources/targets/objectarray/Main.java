import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
    public static void main(String[] args) {
        Object[] small = new Object[10];
        small[7] = new Object();
        int i = Verifier.nondetInt();
        if (i >= 0 && i < 10) {
            Object o = small[i];
            small[i] = null;
        }
        Object[] one = new Object[1];
        if (i >= 0 && i < 10) {
            one[i - 7] = null;
        }
        if (args.length > 0 && i >= 0 && i < 10) {
            Object[] large = new Object[(1 << 16) + 1];
            large[i] = null;
        }
    }
}
