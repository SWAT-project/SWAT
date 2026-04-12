package de.uzl.its.tests;

public class DXXX {

    /**
     * Test function for DADD
     *
     * @param d (symbolic) input
     * @return The result of the addition
     */
    public int DADD(double d) {
        double f = d + 10.0d;
        return f == 10.0d ? 1 : 0;
    }

    /**
     * Test function for DSUB
     *
     * @param d (symbolic) input
     * @return The result of the subtraction
     */
    public int DSUB(double d) {
        double f = d - 10.0d;
        return f == 10.0d ? 1 : 0;
    }

    /**
     * Test function for DDIV
     *
     * @param d (symbolic) input
     * @return The result of the division
     */
    public int DDIV(double d) {
        double f = d / 10.0d;
        return f == 10.0d ? 1 : 0;
    }

    /**
     * Test function for DMUL
     *
     * @param d (symbolic) input
     * @return The result of the multiplication
     */
    public int DMUL(double d) {
        double f = d * 10.0d;
        return f == 10.0d ? 1 : 0;
    }

    /**
     * Test function for DNEG
     *
     * @param d (symbolic) input
     * @return The result of the negation
     */
    public int DNEG(double d) {
        double f = -d;
        return f == 10.0d ? 1 : 0;
    }






    /**
     * Test function for DREM
     *
     * @param d (symbolic) input
     * @return The result of the modulo operation
     */
    public int DREM(double d) {
        double f = d % 100.0d;
        return f == 10.0d ? 1 : 0;
    }


    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        DXXX test = new DXXX();
        double[] testCases = {0.0d, 1.0d, 10.0d, -10.0d, Double.MAX_VALUE, Double.MIN_VALUE};
        for (double testCase : testCases) {
            test.DADD(testCase);
            test.DSUB(testCase);
            test.DDIV(testCase);
            test.DMUL(testCase);
            test.DNEG(testCase);
            test.DREM(testCase);
        }
    }
}
