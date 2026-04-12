package de.uzl.its.tests;
import de.uzl.its.swat.annotations.Symbolic;
import de.uzl.its.uninstrumented.Helper;
public class Main {

    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        @Symbolic int i = 5;
        if(i == Helper.staticInt) {
            System.out.println("Hello, World!");
        } else {
            System.out.println("Goodbye, World!");
        }
        // ensure the ordering of the parameters is identical to the args array
    }

}
