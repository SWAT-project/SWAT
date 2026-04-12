package de.uzl.its.tests.arrays;

import de.uzl.its.swat.annotations.Symbolic;

public class Main {

    /**
     * Main function - acts as the harness for the symbolic execution
     * Tests all primitive array types with symbolic values
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        TestResult result = PrimitiveArrayTests.runSymbolicTests();
        System.out.println(result);

        System.out.println(result.toParsableString());
        System.out.println("✅ TESTS COMPLETED");
    }
}
