package de.uzl.its.swat.instrument.rewrite;

import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.instrument.TransformerType;
import de.uzl.its.swat.instrument.nocache.NoCacheTransformer;
import de.uzl.its.swat.instrument.refequality.RefEqualityTransformer;
import de.uzl.its.swat.instrument.svcomp.SVCompTransformer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * SWAT's semantic rewrites: the changes to a class that come before the instrumentation and alter
 * what runs (de-interned strings and boxes, reference comparisons through {@code
 * UtilInstrumented.refEquals}, SV-COMP's {@code Verifier} calls redirected to SWAT's). There is one
 * implementation, used in two places:
 *
 * <ul>
 *   <li>ahead of time, by {@link de.uzl.its.swat.prepare.Prepare}, which writes the rewritten
 *       classes of a classpath (the prepared classes) for the static analysis and the executor;
 *   <li>at load time, by {@link RewriteTransformer}, for every class that was not prepared
 *       (generated at run time, from jars, from other classpaths).
 * </ul>
 *
 * Both run the same steps in the same order on the same bytes, so a prepared class equals what the
 * load-time chain makes of the original (the invariant {@code PrepareInvariant} checks). The steps
 * must therefore be deterministic: no counters across classes, nothing that depends on load order.
 */
public final class Rewrites {

    /** Version of the prepared-class format; bump it when the rewrites or the marker change. */
    public static final int FORMAT_VERSION = 2;

    private Rewrites() {}

    /** The rewrite steps for the configured transformer type, in the order the agent runs them. */
    public static List<ClassFileTransformer> steps() {
        List<ClassFileTransformer> steps = new ArrayList<>();
        steps.add(new NoCacheTransformer());
        steps.add(new RefEqualityTransformer());
        if (Config.instance().getInstrumentationTransformer() == TransformerType.SV_COMP) {
            steps.add(new SVCompTransformer());
        }
        return steps;
    }

    /**
     * Applies the steps to a class.
     *
     * @return the rewritten class, or {@code cbuf} itself if no step changed it
     */
    public static byte[] apply(List<ClassFileTransformer> steps, ClassLoader loader, String cname, byte[] cbuf)
            throws IllegalClassFormatException {
        byte[] cur = cbuf;
        for (ClassFileTransformer step : steps) {
            byte[] out = step.transform(loader, cname, null, null, cur);
            if (out != null) {
                cur = out;
            }
        }
        return cur;
    }

    /**
     * A hash of everything the rewrites' output depends on besides the class itself: the format
     * version and the configuration they read. A prepared class carries it in its marker; the
     * agent refuses a prepared class whose hash differs from its own.
     */
    public static String configHash() {
        Config c = Config.instance();
        String s = "format=" + FORMAT_VERSION
                + ";transformer=" + c.getInstrumentationTransformer()
                + ";include=" + String.join(",", c.getInstrumentationScope())
                + ";exclude=" + String.join(",", c.getInstrumentationExcludePackages())
                + ";stringInterning=" + c.isUseStringInterning();
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
