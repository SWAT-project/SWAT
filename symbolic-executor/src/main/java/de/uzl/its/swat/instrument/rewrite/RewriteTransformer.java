package de.uzl.its.swat.instrument.rewrite;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.Util;
import de.uzl.its.swat.common.exceptions.InstrumentationException;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.instrument.Transformer;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Applies SWAT's semantic rewrites ({@link Rewrites}) at load time, to every class that was not
 * prepared ahead of time. A prepared class (one with a {@link PreparedMarker} that matches this
 * agent's config) is passed on unchanged: it has been rewritten already, and a class must never be
 * rewritten twice. A prepared class whose marker does not match is an error, since it was
 * rewritten differently from what this run expects.
 *
 * <p>For tests: {@code -Dswat.dumpRewritten=<dir>} writes every class this transformer rewrites to
 * {@code <dir>/<name>.class} (see {@code PrepareInvariant}).
 */
public class RewriteTransformer implements ClassFileTransformer {

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    /** Classes loaded already prepared, and classes rewritten at load time (logged at shutdown). */
    public static final AtomicInteger PREPARED = new AtomicInteger();
    public static final AtomicInteger REWRITTEN = new AtomicInteger();

    private final List<ClassFileTransformer> steps = Rewrites.steps();
    private final String configHash = Rewrites.configHash();
    private final Path dumpDir;

    public RewriteTransformer() {
        String dump = System.getProperty("swat.dumpRewritten");
        dumpDir = dump == null || dump.isEmpty() ? null : Path.of(dump);
        Transformer.getPrintBox().addMsg("Initializing Transformer: " + getClass().getSimpleName()
                + " (" + steps.size() + " rewrites, config " + configHash + ")");
    }

    @Override
    public byte[] transform(
            ClassLoader loader, String cname, Class<?> classBeingRedefined, ProtectionDomain d, byte[] cbuf) {
        if (classBeingRedefined != null || cname == null) return null;
        try {
            if (Util.shouldInstrument(cname)) {
                PreparedMarker.Marker marker = PreparedMarker.read(cbuf);
                if (marker != null) {
                    if (!marker.matches(configHash)) {
                        throw new IllegalStateException("Class " + cname + " was prepared with format "
                                + marker.version() + " and config " + marker.config() + ", but this agent uses format "
                                + Rewrites.FORMAT_VERSION + " and config " + configHash
                                + "; refusing to run it (it must not be rewritten twice)");
                    }
                    PREPARED.incrementAndGet();
                    return null;
                }
            }
        } catch (Throwable t) {
            new ErrorHandler().handleException(new InstrumentationException(cname, t));
            return null;
        }
        byte[] out;
        try {
            out = Rewrites.apply(steps, loader, cname, cbuf);
        } catch (Exception e) {
            // The steps handle their own errors; this only covers the declared exception
            new ErrorHandler().handleException("Error while rewriting class: " + cname, e);
            return null;
        }
        if (out == cbuf) {
            return null;
        }
        REWRITTEN.incrementAndGet();
        if (dumpDir != null) {
            try {
                Path f = dumpDir.resolve(cname + ".class");
                Files.createDirectories(f.getParent());
                Files.write(f, out);
            } catch (IOException e) {
                logger.warn("Could not dump the rewritten class {}: {}", cname, e.toString());
            }
        }
        return out;
    }
}
