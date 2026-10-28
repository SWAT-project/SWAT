import org.sosy_lab.sv_benchmarks.Verifier;

/** Integer.toString(int, int) with a symbolic number and a symbolic radix (see RadixChoiceAgentSpec). */
public class Main {
    public static void main(String[] args) {
        int x = Verifier.nondetInt();
        int radix = Verifier.nondetInt();
        String s = Integer.toString(x, radix);
        if (s.equals("-zz")) {
            System.out.println("found");
        }
    }
}
