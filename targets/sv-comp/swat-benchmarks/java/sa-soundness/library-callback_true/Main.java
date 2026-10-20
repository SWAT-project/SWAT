// Expected verdict: SAFE. the assert needs x > 1000 && x < 1001 for an int x: unsatisfiable.
// Paired VIOLATION inputs (must not fail here): int:1001 int:7
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import org.sosy_lab.sv_benchmarks.Verifier;
// Library code calls back into application code: PushbackReader.read() calls the application's
// Reader, whose read() branches. The graph has the call as a plain library call, so the walk has
// to step over the callback's branches (from nanoxml, java-ranger-regression).
public class Main {
    static class Chars extends Reader {
        private final int[] data;
        private int next = 0;

        Chars(int[] data) {
            this.data = data;
        }

        public int read() {
            if (next >= data.length) {
                return -1;
            }
            int c = data[next];
            next = next + 1;
            return c > 100 ? 100 : c;
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
        PushbackReader in = new PushbackReader(new Chars(new int[] {b, 5}));
        int c = in.read();
        if (c == 7) {
            if (a > 1000 && a < 1001) assert false;
        }
        if (c > 50) c--;
    }
}
