// Expected verdict: SAFE. a port outside 1..65535 is replaced by the default before the check.
// Paired VIOLATION inputs (must not fail here): int:70000
import org.sosy_lab.sv_benchmarks.Verifier;

// Starts a server from its configuration: a listening port and 16 feature flags. The enabled
// features are registered and logged; then the server binds to the port.
public class Main {
    static final int FLAGS = 16;
    static final int DEFAULT_PORT = 8080;

    static boolean[] enabled = new boolean[FLAGS];
    static int enabledCount, warnings, bound;

    static void enable(int flag, int value) {
        enabled[flag] = true;
        enabledCount++;
        if (value != 1) warnings++; // flags should be 0 or 1; other values count as on
        System.out.println(flag);
    }

    static void bind(int port) {
        bound = port;
    }

    public static void main(String[] args) {
        int port = Verifier.nondetInt();
        int[] flags = new int[FLAGS];
        for (int i = 0; i < FLAGS; i++) {
            flags[i] = Verifier.nondetInt();
        }
        for (int i = 0; i < FLAGS; i++) {
            if (flags[i] != 0) {
                enable(i, flags[i]);
            }
        }
        System.out.println(enabledCount);
        if (port <= 0 || port > 65535) port = DEFAULT_PORT; // unset or invalid
        assert port > 0 && port <= 65535 : "invalid port";
        bind(port);
    }
}
