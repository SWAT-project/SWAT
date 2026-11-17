// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:boom
// A custom Handler (application code, run by Logger.warning) asserts on the message: an
// ignored or context-free Logger call must not hide it.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        Logger logger = Logger.getLogger("swat.model-soundness");
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            public void publish(LogRecord r) {
                if (r.getMessage().equals("boom")) assert false;
            }
            public void flush() {}
            public void close() {}
        });
        String s = Verifier.nondetString();
        logger.warning(s);
    }
}
