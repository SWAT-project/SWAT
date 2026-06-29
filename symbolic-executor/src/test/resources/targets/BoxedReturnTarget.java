import de.uzl.its.swat.annotations.Symbolic;

/**
 * Level-2 target for G3-A2 (boxed output-boundary de-interning): an instrumented method calls
 * un-instrumented (java/lang) methods whose DECLARED return type is a boxed wrapper NOT matched by the
 * existing valueOf(primitive) rewrites - Integer.valueOf(String) (category-1) and Long.valueOf(String)
 * (category-2, the wide-local rebox path). Under the real agent these trigger the unbox+rebox de-intern
 * wrap; the program must load, verify, and run to completion - a malformed wrap would VerifyError into a
 * non-zero exit, which AgentRun asserts against. See docs/heap-redesign-g3-design.md (Level L2).
 */
public class BoxedReturnTarget {

    public static void main(String[] args) {
        test(5);
    }

    public static String test(@Symbolic int x) {
        Integer i = Integer.valueOf(Integer.toString(x)); // Integer return -> category-1 unbox+rebox
        Long l = Long.valueOf(Long.toString((long) x));   // Long return    -> category-2 wide unbox+rebox
        if (i.intValue() + l.longValue() > 0) {
            return "pos";
        }
        return "nonpos";
    }
}
