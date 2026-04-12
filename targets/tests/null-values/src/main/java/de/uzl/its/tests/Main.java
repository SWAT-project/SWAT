package de.uzl.its.tests;


public class Main {
    public static Integer nullableInteger;
    public static int i;
    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        System.out.println(nullableInteger);
        System.out.println(i);
    }

}
