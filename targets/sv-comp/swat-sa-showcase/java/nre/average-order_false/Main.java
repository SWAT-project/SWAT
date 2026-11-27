// Expected verdict: VIOLATION (no-runtime-exception). Violating inputs (nondet call order): int:1000
import org.sosy_lab.sv_benchmarks.Verifier;

// The average order size over the last 1000 orders of a shop, ignoring the orders before a
// cutoff (e.g. the ones placed before a price change). Shops with large averages get a badge.
public class Main {
    static final int ORDERS = 1000;

    public static void main(String[] args) {
        int cutoff = Verifier.nondetInt();
        if (cutoff < 0) cutoff = 0;
        int counted = 0;
        int items = 0;
        for (int i = 0; i < ORDERS; i++) {
            if (i >= cutoff) {
                counted++;
                items += 3; // the shop's orders: 3 items each
            }
        }
        if (items / counted > 2) {
            System.out.println("badge: large orders");
        }
    }
}
