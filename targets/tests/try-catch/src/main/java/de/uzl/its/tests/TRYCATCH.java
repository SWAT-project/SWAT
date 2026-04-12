package de.uzl.its.tests;

public class TRYCATCH {

    /**
     * Test function for I2B
     *
     * @param i (symbolic) input
     * @return the converted value
     */
    public int TRY_EX(int i) {
        try {
            i = helper_ex(i);
        } catch (Throwable e) {
            i += 41;
        }
        return i == 0 ? 1 : 0;
    }

    public int helper_ex(int i) {
        i += 42;
        throw new NullPointerException();
    }

    public int helper_ex2(int i) throws NullPointerException {
        i = 42;
        throw new NullPointerException();
    }
    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        TRYCATCH test = new TRYCATCH();
        int[] testCases = {1}; // , 0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int testCase : testCases) {
            System.out.println("Test case " + testCase + " started");
            int res = test.TRY_EX(testCase);
            System.out.println("Test case " + testCase + " finished" + " with result: " + res);
        }
    }
}
