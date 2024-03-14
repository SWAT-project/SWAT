package de.uzl.its.tests;

public class Main {

    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        String symbolicString = args[0];
        test(symbolicString);
        // ensure the ordering of the parameters is identical to the args array
    }

    public static void test(String symbolicString) {
        assert !symbolicString.equals(String.valueOf(Helper.x));
    }
}
