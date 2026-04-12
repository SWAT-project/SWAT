package de.uzl.its.tests;

public class Main {

    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        int ignore = test(1, 1);
        // ensure the ordering of the parameters is identical to the args array
    }

    public static int helper(Integer i, Integer j) {
        return i + j == 42 ? 1 : 0;
    }

    public static int test(Integer i, Integer j) {
        i = i + 10;
        j = j - 10;
        System.out.println("i = " + i + ", j = " + j);
        return helper(i, j);
    }
}
