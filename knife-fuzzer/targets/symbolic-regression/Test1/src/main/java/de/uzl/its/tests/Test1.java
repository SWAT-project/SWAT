package de.uzl.its.tests;

public class Test1 {

    /**
     * Test function for I2B
     *
     * @param i (symbolic) input
     * @return the converted value
     */
    public int I2B(int i) {
        byte b = (byte) i;
        return b == 0 ? 1 : 0;
    }

    /**
     * Test function for I2C
     *
     * @param i (symbolic) input
     * @return the converted value
     */
    public int I2C(int i) {
        char c = (char) i;
        return c == 0 ? 1 : 0;
    }

    /**
     * Test function for I2D
     *
     * @param i (symbolic) input
     * @return the converted value
     */
    public int I2D(int i) {
        double d = (double) i;
        return d == 0.0 ? 1 : 0;
    }

    /**
     * Test function for I2F
     *
     * @param i (symbolic) input
     * @return the converted value
     */
    public int I2F(int i) {
        float f = (float) i;
        return f == 0.0F ? 1 : 0;
    }

    /**
     * Test function for I2L
     *
     * @param i (symbolic) input
     * @return the converted value
     */
    public int I2L(int i) {
        long l = (long) i;
        return l == 0L ? 1 : 0;
    }

    /**
     * Test function for I2S
     *
     * @param i (symbolic) input
     * @return the converted value
     */
    public int I2S(int i) {
        short s = (short) i;
        return s == 0 ? 1 : 0;
    }

    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */

    public int felixTest1(int x) {
        return x + 5 > 10 ? 0 : 1;
    }

    public static void main(String[] args) {
        Test1 test = new Test1();
        int[] testCases = {1, 0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int testCase : testCases) {
            felixTest1(testCase);
        }
    }
}
