package de.uzl.its.examples;

public class Basic1 {

    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        String symbolicString = args[0];
        int symbolicInt = Integer.parseInt(args[1]);
        test(
                symbolicString,
                symbolicInt); // ensure the ordering of the parameters is identical to the args
        // array
    }

    public static void test(String symbolicString, int symbolicInt) {
        if (symbolicString.charAt(symbolicInt) == 'X') {
            System.out.println("Branch 1");
            assert symbolicInt != 10;
        } else {
            System.out.println("Branch 2");
        }
    }
}
