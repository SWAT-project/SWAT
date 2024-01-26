package de.uzl.its.tests;

public class XASTORE {

    /**
     * Test function for IASTORE
     *
     * @param i (symbolic) index
     * @param j (symbolic) value
     * @return the converted value
     */
    public int IASTORE(int i, int j) throws IndexOutOfBoundsException {
        int[] ia = new int[10];
        ia[i] = j;
        return ia[5] == 42 ? 1 : 0;
    }

    /**
     * Test function for IASTORE
     *
     * @param i (symbolic) index
     * @return the converted value
     */
    public int IASTORE(int i) throws IndexOutOfBoundsException {
        int[] ia = new int[10];
        ia[i] = 42;
        return ia[5] == 42 ? 1 : 0;
    }
    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        XASTORE test = new XASTORE();
        int[] testCases = {1}; // {1, 0, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int testCase : testCases) {
            test.IASTORE(testCase);
            /*for (int testCase1 : testCases) {
                try{
                    test.IASTORE(testCase, testCase1);
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("IndexOutOfBoundsException");
                }
            }
                */
        }
    }
}
