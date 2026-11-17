// Expected verdict: VIOLATION. Violating inputs (nondet call order): string:boom
// A JDK StreamHandler writes the message into a buffer that the program reads back: the
// Logger call stores the symbolic message.
import org.sosy_lab.sv_benchmarks.Verifier;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class Main {
    public static void main(String[] args) {
        Logger logger = Logger.getLogger("swat.model-soundness.stream");
        logger.setUseParentHandlers(false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamHandler handler = new StreamHandler(out, new SimpleFormatter());
        logger.addHandler(handler);
        String s = Verifier.nondetString();
        logger.warning(s);
        handler.flush();
        if (out.toString().contains("boom")) assert false;
    }
}
