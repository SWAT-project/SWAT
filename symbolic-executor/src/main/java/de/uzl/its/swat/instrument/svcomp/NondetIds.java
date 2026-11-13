package de.uzl.its.swat.instrument.svcomp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The ids of the nondet calls the SV-COMP rewrite redirects to SWAT's {@link Verifier}. An id names
 * the input a call returns ({@code I_<id>}, {@code -Dswat.input.I_<id>=...}), so it must not depend
 * on anything but the call itself: it is derived from the class, the method, its descriptor and
 * the call's ordinal among the nondet calls of the method. Classes rewritten ahead of time (the
 * prepared classpath) and classes rewritten at load time thus get the same ids, whatever the load
 * order.
 *
 * <p>Ids are below {@link #RANGE} (10^12), so that the occurrence suffix of a call that runs several
 * times ({@code <id><nn>}, see {@code InternalInvocation.liftValue}) still fits a long. Over the few
 * hundred nondet calls of a program, a collision (two calls sharing an input) has a probability of
 * about n^2 / 2 * 10^-12.
 */
public final class NondetIds {

    /** Exclusive upper bound of the ids. */
    public static final long RANGE = 1_000_000_000_000L;

    private NondetIds() {}

    /**
     * The id of a nondet call.
     *
     * @param owner the internal name of the class containing the call
     * @param method the name of the method containing the call
     * @param desc the descriptor of that method
     * @param ordinal the call's position among the nondet calls of the method (from 0, in bytecode
     *     order)
     */
    public static long of(String owner, String method, String desc, int ordinal) {
        String site = owner + "." + method + desc + "#" + ordinal;
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(site.getBytes(StandardCharsets.UTF_8));
            long v = 0;
            for (int i = 0; i < 8; i++) {
                v = (v << 8) | (h[i] & 0xff);
            }
            return Long.remainderUnsigned(v, RANGE);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
