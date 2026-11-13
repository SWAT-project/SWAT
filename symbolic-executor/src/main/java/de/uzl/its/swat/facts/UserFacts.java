package de.uzl.its.swat.facts;

import de.uzl.its.swat.instrument.GlobalStateForInstrumentation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hand-written facts from the program's source (the annotations module, de.uzl.its.swat.annotations),
 * read only with {@code userFacts} on (see Config):
 *
 * <ul>
 *   <li>{@code @Irrelevant} on a class or method, and the region between {@code Swat.beginIrrelevant()}
 *       and {@code Swat.endIrrelevant()}: the instrumentation registers the branch instructions in
 *       them here ({@link #register}); the trace flags their branches as merged (no decision for
 *       the explorer, constraint left out) and names the fact.
 *   <li>{@code Swat.summary()}: the class preparation ({@link UserFactsTransformer}) replaces the
 *       call by one to {@link #summary(String)}, which returns true, so the hand-written summary
 *       runs instead of the loop.
 *   <li>{@code @Target}: the class preparation calls {@link #target} at the start of the method.
 * </ul>
 *
 * The facts a run relied on ({@link #used}) go into its trace, so that the explorer can say that a
 * SAFE verdict assumes them.
 */
public final class UserFacts {

    /** Descriptors and names of the annotations module (built at run time: no relocation). */
    public static final String PACKAGE = "de/uzl/its/".concat("swat/annotations/");
    public static final String IRRELEVANT = "L" + PACKAGE + "Irrelevant;";
    public static final String TARGET = "L" + PACKAGE + "Target;";
    public static final String NOT_CALLED_BACK = "L" + PACKAGE + "NotCalledBack;";
    /** The prefix of a fact a run refuted, among the facts it reports (see {@link #refute}). */
    public static final String REFUTED = "REFUTED ";
    public static final String SWAT = PACKAGE + "Swat";

    private static final Map<Long, String> irrelevant = new ConcurrentHashMap<>();
    private static final Set<String> used = ConcurrentHashMap.newKeySet();
    /** The methods annotated {@code @NotCalledBack}, by class and method (as the class depot names them). */
    private static final Set<String> notCalledBack = ConcurrentHashMap.newKeySet();

    private UserFacts() {}

    /** The instruction with this iid is in code a fact declares irrelevant (see the class comment). */
    public static void register(long iid, String fact) {
        irrelevant.put(iid, fact);
    }

    /** The irrelevance fact covering the branch instruction of {@code iid}, or null. */
    public static String irrelevantFact(long iid) {
        return irrelevant.get(GlobalStateForInstrumentation.baseIid(iid));
    }

    /** A method annotated {@code @NotCalledBack} (class and method as the class depot formats them). */
    public static void registerNotCalledBack(String cname, String method) {
        notCalledBack.add(cname + "#" + method);
    }

    /**
     * Library code called back into a method: if it is annotated {@code @NotCalledBack}, the fact
     * is refuted, and the run reports it (the explorer then never says SAFE).
     */
    public static void calledBack(String cname, String method) {
        if (!notCalledBack.isEmpty() && notCalledBack.contains(cname + "#" + method)) {
            refute("@NotCalledBack " + method); // the method names its class
        }
    }

    /** Records that a run showed a fact to be wrong. */
    public static void refute(String fact) {
        used.add(REFUTED + fact);
    }

    /** Records that this run relied on a fact. */
    public static void use(String fact) {
        used.add(fact);
    }

    /** The facts this run relied on, sorted. */
    public static List<String> used() {
        return used.stream().sorted().toList();
    }

    /** Called at the start of a method annotated {@code @Target}: reaching it is a violation. */
    public static void target(String description) {
        throw new AssertionError(description);
    }

    /**
     * Called in place of {@code Swat.summary()} with user facts on: the hand-written summary
     * {@code id} runs instead of its loop.
     */
    public static boolean summary(String id) {
        used.add(id);
        return true;
    }
}
