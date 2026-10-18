package de.uzl.its.swat.instrument;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Names the instructions that report a branch or push a frame, so that the explorer can check its
 * walk of the static pre-analysis graph against the trace, and find its place again.
 *
 * <p>A key is {@code owner.name(desc)|line|kind|ordinal}: the method, the source line, the kind
 * ({@code if}, {@code switch}, {@code arr}, {@code newarr}, {@code div}, or {@code call:name(desc)}
 * for a call), and the instruction's position among those of the same kind on that line, in
 * bytecode order. Bytecode offsets would not do: the graph is built by SootUp, which keeps none.
 * Within a line, though, SootUp's statement order is the bytecode order. The extractor builds the
 * same key from SootUp's statements (and leaves out lines where its view of them differs from the
 * bytecode); change both together.
 *
 * <p>Keys are registered while a method is instrumented, for the iid of the instruction (see
 * {@link GlobalStateForInstrumentation#setPendingKey}).
 */
public final class BranchKeys {

    private static final Map<Long, String> keys = new ConcurrentHashMap<>();

    private BranchKeys() {
    }

    static void register(long iid, String key) {
        keys.put(iid, key);
    }

    /** The key of the instruction that recorded {@code iid} (loop and switch bits are ignored), or null. */
    public static String keyOf(long iid) {
        return keys.get(GlobalStateForInstrumentation.baseIid(iid));
    }
}
