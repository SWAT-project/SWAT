// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import org.sosy_lab.sv_benchmarks.Verifier;
// The assert is in application code that library code calls back (PushbackReader.read() calls
// the application's Reader), on an input-dependent path. The graph does not have the callback, so
// it treats the library call as unable to reach an assert. Known false SAFE with SA (RESULTS.md).
public class Main {
    static int input;

    static class Chars extends Reader {
        public int read() {
            if (input > 1000 && input < 100000) assert false;
            return 1;
        }

        public int read(char[] buf, int off, int len) {
            return -1;
        }

        public void close() {
        }
    }

    public static void main(String[] args) throws IOException {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        input = a;
        if (b == 7) {
            new PushbackReader(new Chars()).read();
        }
        if (a > 3) a--;
    }
}
