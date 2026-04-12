package de.uzl.its.tests;

public class Main {

    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        int ignore = test("Foo", 'd');
        // ensure the ordering of the parameters is identical to the args array
    }

    public static int test(String s1, char c2) {
        String s = new StringBuilder()
                .append(s1)
                .append(c2)
                .toString();
        System.out.println(s);
        if (s.equals("fool")) {
            return 1;
        }  else {
            return 0;
        }
    }
}
