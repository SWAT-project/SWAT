package de.uzl.its.tests;

public class IXXX {

    /**
     * Test function for IADD
     *
     * @param i (symbolic) input
     * @return The result of the addition
     */
    public int IADD(int i) {
        int j = i + 10;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for ISUB
     *
     * @param i (symbolic) input
     * @return The result of the subtraction
     */
    public int ISUB(int i) {
        int j = i - 10;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for IDIV
     *
     * @param i (symbolic) input
     * @return The result of the division
     */
    public int IDIV(int i) {
        int j = i / 10;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for IMUL
     *
     * @param i (symbolic) input
     * @return The result of the multiplication
     */
    public int IMUL(int i) {
        int j = i * 10;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for INEG
     *
     * @param i (symbolic) input
     * @return The result of the negation
     */
    public int INEG(int i) {
        int j = -i;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for IAND
     *
     * @param i (symbolic) input
     * @return The result of the AND operation
     */
    public int IAND(int i) {
        int j = i & 10;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for IOR
     *
     * @param i (symbolic) input
     * @return The result of the OR operation
     */
    public int IOR(int i) {
        int j = i | 10;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for IXOR
     *
     * @param i (symbolic) input
     * @return The result of the XOR operation
     */
    public int IXOR(int i) {
        int j = i ^ 10;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for ISHL
     *
     * @param i (symbolic) input
     * @return The result of the shift left operation
     */
    public int ISHL(int i) {
        int j = i << 1;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for ISHR
     *
     * @param i (symbolic) input
     * @return The result of the shift right operation
     */
    public int ISHR(int i) {
        int j = i >> 1;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for IUSHR
     *
     * @param i (symbolic) input
     * @return The result of the unsigned shift right operation
     */
    public int IUSHR(int i) {
        int j = i >>> 1;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for IREM
     *
     * @param i (symbolic) input
     * @return The result of the modulo operation
     */
    public int IREM(int i) {
        int j = i % 100;
        return j == 10 ? 1 : 0;
    }

    /**
     * Test function for IINC
     *
     * @param i (symbolic) input
     * @return The result of the modulo operation
     */
    public int IINC(int i) {
        i++;
        return i == 10 ? 1 : 0;
    }

    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        IXXX test = new IXXX();
        int[] testCases = {0, 1, 10, -10, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int testCase : testCases) {
            test.IADD(testCase);
            test.ISUB(testCase);
            test.IDIV(testCase);
            test.IMUL(testCase);
            test.INEG(testCase);
            test.IAND(testCase);
            test.IOR(testCase);
            test.IXOR(testCase);
            test.ISHL(testCase);
            test.ISHR(testCase);
            test.IUSHR(testCase);
            test.IREM(testCase);
            test.IINC(testCase);
        }
    }
}
