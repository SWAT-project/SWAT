package de.uzl.its.swat.instrument.loopfacts;

import de.uzl.its.cfg.loops.LoopFacts;
import de.uzl.its.cfg.loops.LoopFacts.MethodFacts;
import de.uzl.its.cfg.loops.LoopFactsAnnotations;
import de.uzl.its.cfg.loops.LoopFactsJson;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.config.Config;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;

/**
 * The loop facts of the methods the executor instruments: their loop heads and loop summaries.
 *
 * <p>The static analysis (cfg-extraction) computes them ahead of time. It writes them into the
 * prepared classes as a method annotation ({@link LoopFactsAnnotations}) and to a file ({@code
 * loopFacts}, see Config, used when the classes were not prepared). A method gets them from its
 * annotation, else from the file, if they were computed from the same code (the hash matches);
 * any other method (a class generated at run time, a class outside the analysed classpath, no
 * analysis at all) gets them computed now, by the same library. Either way they are the same facts,
 * so annotations and file only save time, and the static analysis owns the analysis.
 */
public final class LoopFactsSource {

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private static volatile Map<String, MethodFacts> file;

    /** Methods whose facts came from an annotation, the file, were computed, or were stale. */
    private static final AtomicInteger fromAnnotation = new AtomicInteger();
    private static final AtomicInteger fromFile = new AtomicInteger();
    private static final AtomicInteger computed = new AtomicInteger();
    private static final AtomicInteger stale = new AtomicInteger();

    private LoopFactsSource() {}

    /** The facts of a method of {@code owner}, from the file if it has them for this code. */
    public static MethodFacts of(String owner, MethodNode mn) {
        MethodFacts annotated = LoopFactsAnnotations.read(mn);
        if (annotated != null) {
            if (annotated.hash().equals(LoopFacts.hash(mn))) {
                fromAnnotation.incrementAndGet();
                logger.debug("[LOOP FACTS] {}.{}{} from its annotation", owner, mn.name, mn.desc);
                return annotated;
            }
            stale.incrementAndGet();
            logger.debug("[LOOP FACTS] {}.{}{} changed since it was annotated", owner, mn.name, mn.desc);
        }
        MethodFacts known = file().get(LoopFacts.key(owner, mn.name, mn.desc));
        if (known != null) {
            if (known.hash().equals(LoopFacts.hash(mn))) {
                fromFile.incrementAndGet();
                logger.debug("[LOOP FACTS] {}.{}{} from the facts file", owner, mn.name, mn.desc);
                return known;
            }
            stale.incrementAndGet();
            logger.debug("[LOOP FACTS] {}.{}{} changed since the analysis, computing its facts", owner, mn.name, mn.desc);
        } else {
            logger.debug("[LOOP FACTS] {}.{}{} is not in the facts file, computing its facts", owner, mn.name, mn.desc);
        }
        computed.incrementAndGet();
        return LoopFacts.analyze(owner, mn);
    }

    /** How many methods got their facts from annotations, the file, computed, and how many were stale. */
    public static String stats() {
        return fromAnnotation.get() + " from annotations, " + fromFile.get() + " from the file, " + computed.get() + " computed (" + stale.get() + " changed)";
    }

    private static Map<String, MethodFacts> file() {
        Map<String, MethodFacts> f = file;
        if (f == null) {
            synchronized (LoopFactsSource.class) {
                if ((f = file) == null) {
                    file = f = load(Config.instance().getLoopFacts());
                }
            }
        }
        return f;
    }

    private static Map<String, MethodFacts> load(String path) {
        if (path == null || path.isEmpty()) {
            return Map.of();
        }
        try (Reader r = Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)) {
            Map<String, MethodFacts> facts = LoopFactsJson.read(r);
            if (facts == null) {
                logger.warn("[LOOP FACTS] {} has another version than {}, computing all facts", path, LoopFacts.VERSION);
                return Map.of();
            }
            logger.info("[LOOP FACTS] Read the facts of {} methods from {}", facts.size(), path);
            return facts;
        } catch (IOException | RuntimeException e) {
            logger.warn("[LOOP FACTS] Cannot read {} ({}), computing all facts", path, e.toString());
            return Map.of();
        }
    }
}
