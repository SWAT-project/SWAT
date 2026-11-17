// Expected verdict: VIOLATION. Violating inputs (nondet call order): byte:-128
// Arrays.equals(byte[], byte[]) with sign-extended elements: (byte) 255 == -1. The SAFE
// variant has (byte) 254 == -2 against -1.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        byte x = Verifier.nondetByte();
        byte[] a = {x, (byte) -1};
        byte[] b = {(byte) -128, (byte) 255};
        if (Arrays.equals(a, b)) assert false;
    }
}
