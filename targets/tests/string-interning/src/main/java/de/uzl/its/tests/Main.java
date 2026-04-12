package de.uzl.its.tests;

import de.uzl.its.swat.annotations.Symbolic;

public class Main {

    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {

        String var3 = "World".intern();
        String var4 = "World".intern();
        System.out.println("s3 == s4: " + (var3 == var4));
        @Symbolic String var5 = "Hello";
        @Symbolic String var6 = "Hello";
        System.out.println("s1 == s2: " + (var5 == var6));
    }
}
