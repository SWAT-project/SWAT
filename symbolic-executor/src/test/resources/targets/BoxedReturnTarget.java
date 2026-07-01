import de.uzl.its.swat.annotations.Symbolic;

/**
 * Exercises the de-intern instrumentation for the boxed wrapper types under the JVM verifier. A run
 * that completes at exit 0 with a parsed TraceDTO confirms the wrap/rewrite bytecode is valid; a
 * malformed rewrite would raise a VerifyError.
 *
 * <ul>
 *   <li><b>Wrapper returns from factories</b> - declared-wrapper returns from un-instrumented
 *       {@code valueOf(String)} factories: Integer and Long.</li>
 *   <li><b>valueOf rewrite</b> - autoboxing emits {@code <Wrapper>.valueOf(primitive)}, which the
 *       adapter rewrites to {@code new <Wrapper>(primitive)} for all six wrappers (Integer, Long,
 *       Short, Byte, Character, Boolean). Constructing all six covers every per-wrapper descriptor,
 *       especially the short/byte/char descriptors paired with int-category opcodes.</li>
 * </ul>
 *
 * <p>The autoboxed wrappers are kept concrete and consumed via string concatenation, and the
 * {@code valueOf(String)} factories are limited to Integer and Long. Feeding a symbolic value through
 * Short/Byte/Character/Boolean parsing or unboxing hits gaps in the symbolic modeling of those
 * wrappers that are unrelated to the de-intern bytecode validity this target checks.
 */
public class BoxedReturnTarget {

    public static void main(String[] args) {
        test(5);
    }

    public static String test(@Symbolic int x) {
        // Declared-wrapper returns from un-instrumented valueOf(String) factories.
        Integer i = Integer.valueOf(Integer.toString(x));
        Long l = Long.valueOf(Long.toString((long) x));

        // Autoboxed wrappers, kept concrete and consumed via string concatenation below.
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
