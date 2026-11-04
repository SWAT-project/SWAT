// Expected verdict: VIOLATION. Violating inputs (nondet call order): int:1001 int:7
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import org.sosy_lab.sv_benchmarks.Verifier;
// As library-callback, but the callback throws: the exception comes out of the library call into
// the application's handler, where the assert is. (The input is read from a static field: an
// object handed to library code with symbolic fields counts as context loss.)
public class Main {
    static int first;

    static class Chars extends Reader {
        public int read() throws IOException {
            if (first == 7) {
                throw new IOException("end");
            }
            return first > 100 ? 100 : first;
        }

        public int read(char[] buf, int off, int len) {
            return -1;
        }

        public void close() {
        }
    }

    public static void main(String[] args) {
        int a = Verifier.nondetInt();
        int b = Verifier.nondetInt();
        first = b;
        int c;
        try {
            c = new PushbackReader(new Chars()).read();
        } catch (IOException e) {
            if (a > 1000 && a < 100000) assert false;
            c = 0;
        }
        if (c > 50) c--;
    }
}
