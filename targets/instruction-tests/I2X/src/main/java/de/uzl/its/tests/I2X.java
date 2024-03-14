package de.uzl.its.tests;

public class I2X {

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

    public boolean test(float f) {
        double d = 0.1;
        System.out.println(Double.doubleToRawLongBits(d));
        return f != d;
    }

    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        I2X test = new I2X();
        test.test(0);
        /*
        int[] testCases = {1, 0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int testCase : testCases) {
            test.I2B(testCase);
            test.I2C(testCase);
            test.I2D(testCase);
            test.I2F(testCase);
            test.I2L(testCase);
            test.I2S(testCase);
        }

         */
    }
}
