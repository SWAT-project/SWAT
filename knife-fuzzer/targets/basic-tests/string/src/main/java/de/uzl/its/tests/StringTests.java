package de.uzl.its.tests;

public class StringTests {

    /**
     * Test function for charAt with a symbolic index and constant string
     *
     * @param i (symbolic) int
     * @return the converted value
     */
    private int test_charAtSymbolicIdx(int i) {
        String s = "Hello World!";
        return s.charAt(i) == 'o' ? 1 : 0;
    }

    /**
     * Test function for charAt with a symbolic string and constant index
     *
     * @param s (symbolic) string
     * @return the converted value
     */
    private int test_charAtSymbolicString(String s) {
        int i = 10;
        return s.charAt(i) == 'X' ? 1 : 0;
    }

    /**
     * Test function for charAt with a symbolic string and index
     *
     * @param s (symbolic) string
     * @param i (symbolic) index
     * @return the converted value
     */
    private int test_charAt(String s, int i) {
        return s.charAt(i) == 'X' ? 1 : 0;
    }

    /**
     * Test function for concat with one symbolic string (first argument)
     *
     * @param s (symbolic) string
     * @return the concatenated value
     */
    private int test_concatFirstSymbolicArg(String s) {
        return s.concat("World!").equals("Hello World!") ? 1 : 0;
    }

    /**
     * Test function for concat with one symbolic string (first argument)
     *
     * @param s (symbolic) string
     * @return the concatenated value
     */
    private int test_concatSecondSymbolicArg(String s) {
        return "Hello".concat(s).equals("Hello World!") ? 1 : 0;
    }

    /**
     * Test function for concat with two symbolic strings (both arguments) Todo (Nils) This test is
     * not working yet (?)
     *
     * @param s1 (symbolic) string
     * @param s2 (symbolic) string
     * @return the concatenated value
     */
    private int test_concat(String s1, String s2) {
        return s1.concat(s2).equals("Hello World!") ? 1 : 0;
    }

    public static void main(String[] args) {
        StringTests test = new StringTests();
        test.test_charAtSymbolicIdx(1);
        test.test_charAtSymbolicString("Hello World!");
        test.test_charAt("Hello World!", 3);
        test.test_concatFirstSymbolicArg("xxx");
        test.test_concatSecondSymbolicArg("xxx");
        test.test_concat("xxx", "xxx");
    }
}
