// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import org.sosy_lab.sv_benchmarks.Verifier;
// A switch on an enum (synthetic $SwitchMap class with its own <clinit>) selects the asserting case.
public class Main {
    enum Op { START, STOP, STATUS }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        Op op = b == 1 ? Op.START : b == 7 ? Op.STOP : Op.STATUS;
        int r;
        switch (op) {
            case START: r = 1; break;
            case STOP:
                if (a > 1000 && a < 100000) assert false;
                r = 2;
                break;
            default: r = 3;
        }
        if (r > 2) r--;
    }
}
