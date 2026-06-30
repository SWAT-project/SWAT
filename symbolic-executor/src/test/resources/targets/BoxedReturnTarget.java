import de.uzl.its.swat.annotations.Symbolic;

/**
 * Level-2 target for G3-A2 (boxed output-boundary de-interning), driving the de-intern instrumentation
 * for the wrapper types under the real JVM verifier (the run completing at exit 0 with a parsed TraceDTO
 * is the bytecode-validity oracle; a malformed wrap/rewrite would VerifyError).
 *
 * <ul>
 *   <li><b>deInternReturn (unbox+rebox) branch</b> - declared-wrapper returns from un-instrumented
 *       {@code valueOf(String)} factories: Integer (category-1) and Long (category-2 wide-local).</li>
 *   <li><b>valueOf rewrite branch</b> - autoboxing emits {@code <Wrapper>.valueOf(primitive)}, which the
 *       adapter rewrites to {@code new <Wrapper>(primitive)} for ALL SIX wrappers (Integer/Long/Short/
 *       Byte/Character/Boolean). Per-wrapper descriptors are DERIVED from the {@code Boxed} enum, so
 *       verifier coverage of every type - especially the short/byte/char descriptors paired with
 *       int-category opcodes - is the point of constructing all six here.</li>
 * </ul>
 *
 * <p>The autoboxed wrappers are kept CONCRETE and consumed via string concatenation ({@code toString}),
 * and the {@code valueOf(String)} factories are limited to Integer/Long. This is deliberate: feeding a
 * symbolic value through Short/Byte/Character/Boolean parsing or unboxing trips unrelated gaps in the
 * symbolic modeling of those wrappers, which are out of scope for this de-intern bytecode-validity test.
 * Object-identity (the de-intern effect) is pinned at L1 by OutputDeInternSpec, the boxed-cache
 * {@code ==} semantics at L0 by ProvenanceRefEqualsSpec. See docs/heap-redesign-g3-design.md (Level L2).
 */
public class BoxedReturnTarget {

    public static void main(String[] args) {
        test(5);
    }

    public static String test(@Symbolic int x) {
        // deInternReturn branch: declared-wrapper returns from un-instrumented valueOf(String) factories.
        Integer i = Integer.valueOf(Integer.toString(x));         // category-1 unbox+rebox
        Long l = Long.valueOf(Long.toString((long) x));           // category-2 wide unbox+rebox

        // valueOf rewrite branch: autoboxing -> <Wrapper>.valueOf(primitive) -> new <Wrapper>(primitive),
        // for all six wrappers. Concrete; consumed via string concatenation below (no symbolic unbox).
        Integer ai = 11;
        Long al = 12L;
        Short ash = (short) 13;
        Byte aby = (byte) 14;
        Character ach = 'c';
        Boolean abo = true;

        if (i.intValue() + l.longValue() > 0) {
            return "pos:" + ai + al + ash + aby + ach + abo;
        }
        return "nonpos";
    }
}
