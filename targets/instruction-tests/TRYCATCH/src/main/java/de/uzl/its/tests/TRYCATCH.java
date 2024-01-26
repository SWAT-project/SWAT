package de.uzl.its.tests;

public class TRYCATCH {

    /**
     * Test function for catching (Arithmetic)exceptions, no branching in catch block
     *
     * @param i (symbolic) input
     * @return the resulting value
     */
    public int IDIV(int i) {
        try {
            return 10 / i;
        } catch (ArithmeticException e) {
            return 0;
        }
    }

    /**
     * Test function for catching (Arithmetic)exceptions, no branching in catch block
     *
     * @param i (symbolic) input
     * @return the resulting value
     */
    public int IREM(int i) {
        try {
            return 10 % i;
        } catch (ArithmeticException e) {
            return 0;
        }
    }
    /**
     * Test function for catching (Arithmetic)exceptions, no branching in catch block
     *
     * @param i (symbolic) input
     * @return the resulting value
     */
    public long LDIV(long i) {
        try {
            return 10l / i;
        } catch (ArithmeticException e) {
            return 0l;
        }
    }

    /**
     * Test function for catching (Arithmetic)exceptions, no branching in catch block
     *
     * @param i (symbolic) input
     * @return the resulting value
     */
    public long LREM(long i) {
        try {
            return 10l % i;
        } catch (ArithmeticException e) {
            return 0l;
        }
    }

    /**
     * Test function for catching (Arithmetic)exceptions, branching in catch block
     *
     * @param i (symbolic) input
     * @return the resulting value
     */
    public int IDIV(int i, int j) {
        try {
            return j / i;
        } catch (ArithmeticException e) {
            return j == 42 ? 1 : 0;
        }
    }

    /**
     * Test function for catching (Arithmetic)exceptions, branching in catch block
     *
     * @param i (symbolic) input
     * @return the resulting value
     */
    public int IREM(int i, int j) {
        try {
            return j % i;
        } catch (ArithmeticException e) {
            return j == 42 ? 1 : 0;
        }
    }

    /**
     * Test function for catching (Arithmetic)exceptions, branching in catch block
     *
     * @param i (symbolic) input
     * @return the resulting value
     */
    public long LDIV(long i, long j) {
        try {
            return 10l / i;
        } catch (ArithmeticException e) {
            return j == 42l ? 1l : 0l;
        }
    }

    /**
     * Test function for catching (Arithmetic)exceptions, branching in catch block
     *
     * @param i (symbolic) input
     * @return the resulting value
     */
    public long LREM(long i, long j) {
        try {
            return 10l % i;
        } catch (ArithmeticException e) {
            return j == 42l ? 1l : 0l;
        }
    }

    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        TRYCATCH test = new TRYCATCH();
        int[] testCases = {1, 0}; // , 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};

        for (int testCase : testCases) {
            test.IDIV(testCase);
            test.IREM(testCase);
            test.LDIV(testCase);
            test.LREM(testCase);
            for (int testCase2 : testCases) {
                test.IDIV(testCase, testCase2);
                test.IREM(testCase, testCase2);
                test.LDIV(testCase, testCase2);
                test.LREM(testCase, testCase2);
            }
        }
    }
}
