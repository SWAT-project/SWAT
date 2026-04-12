package de.uzl.its.tests;

import de.uzl.its.tests.exclude.UtilExcl;

public class INVOKESTATIC {

    /**
     * Test function for static invocation of a non instrumented method without parameters
     *
     * @param i (symbolic) input
     * @return 1 if condition is satisfied, 0 otherwise
     */
    public int INVOKESTATIC1(int i) {
        return i != UtilExcl.constant() ? 1 : 0;
    }
    /**
     * Test function for static invocation of a non instrumented method without parameters. The
     * un-instrumented static method retrieves a static constant from an instrumented class that has
     * not been loaded.
     *
     * @param i (symbolic) input
     * @return 1 if condition is satisfied, 0 otherwise
     */
    public int INVOKESTATIC2(int i) {
        return i != UtilExcl.constant2() ? 1 : 0;
    }
    /**
     * Test function for static invocation of a non instrumented method without parameters The
     * un-instrumented static method calls an instrumented static method from a different class
     *
     * @param i (symbolic) input
     * @return 1 if condition is satisfied, 0 otherwise
     */
    public int INVOKESTATIC3(int i) {
        return i != UtilExcl.constant3() ? 1 : 0;
    }

    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        INVOKESTATIC test = new INVOKESTATIC();
        int[] testCases = {1}; // , 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int testCase : testCases) {
            test.INVOKESTATIC1(testCase);
            test.INVOKESTATIC2(testCase);
            test.INVOKESTATIC3(testCase);
        }
    }
}
