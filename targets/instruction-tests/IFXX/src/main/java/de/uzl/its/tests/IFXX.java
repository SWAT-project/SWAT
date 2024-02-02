package de.uzl.its.tests;

public class IFXX {

    /**
     * Test function for IFEQ
     *
     * @param i (symbolic) input
     * @return 1 if condition is satisfied, 0 otherwise
     */
    public int IFEQ(int i) {
        return i != 0 ? 1 : 0;
    }

    /**
     * Test function for IFNE
     *
     * @param i (symbolic) input
     * @return 1 if condition is satisfied, 0 otherwise
     */
    public int IFNE(int i) {
        return i == 0 ? 1 : 0;
    }

    /**
     * Test function for IFLT
     *
     * @param i (symbolic) input
     * @return 1 if condition is satisfied, 0 otherwise
     */
    public int IFLT(int i) {
        return i >= 0 ? 1 : 0;
    }

    /**
     * Test function for IFGT
     *
     * @param i (symbolic) input
     * @return 1 if condition is satisfied, 0 otherwise
     */
    public int IFGT(int i) {
        return i <= 0 ? 1 : 0;
    }

    /**
     * Test function for IFLE
     *
     * @param i (symbolic) input
     * @return 1 if condition is satisfied, 0 otherwise
     */
    public int IFLE(int i) {
        return i > 0 ? 1 : 0;
    }

    /**
     * Test function for IFGE
     *
     * @param i (symbolic) input
     * @return 1 if condition is satisfied, 0 otherwise
     */
    public int IFGE(int i) {
        return i < 0 ? 1 : 0;
    }

    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        IFXX test = new IFXX();
        int[] testCases = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int testCase : testCases) {
            test.IFEQ(testCase);
            test.IFNE(testCase);
            test.IFLT(testCase);
            test.IFGT(testCase);
            test.IFLE(testCase);
            test.IFGE(testCase);
        }
    }
}
