import de.uzl.its.swat.annotations.Symbolic;

/**
 * A char-sorted shadow value flowing into an int-slot whitelisted call. {@code charAt} is modeled
 * and yields a genuinely char-sorted symbolic value; the char-to-int widening emits no bytecode, so
 * that value reaches {@code Character.isWhitespace(int)} with a sort deviating from the descriptor.
 * Such a call must fall back to concretization (recording context loss), NOT reuse the cached
 * {@code pure_Character_isWhitespace_int} declaration with a deviating signature (which would be a
 * solver error). The same method over a genuine int-sorted value (the addition forces one) must
 * still be modeled as the UF. (A {@code @Symbolic char} parameter would not do: input designation
 * lifts it as an int.)
 */
public class CharWideningTarget {

    public static void main(String[] args) {
        test("x");
    }

    public static int test(@Symbolic String s) {
        char c = s.charAt(0); // modeled: a genuinely char-sorted symbolic value
        int r = 0;
        if (Character.isWhitespace((int) c)) r++; // char-sorted shadow in an int slot: concretized + flagged
        if (Character.isWhitespace(c + 0)) r++;   // the addition yields an int-sorted value: modeled as the UF
        return r;
    }
}
