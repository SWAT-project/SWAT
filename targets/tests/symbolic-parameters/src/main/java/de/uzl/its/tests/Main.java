package de.uzl.its.tests;

import de.uzl.its.swat.annotations.Symbolic;

import java.util.ArrayList;
import java.util.Objects;

public class Main {


    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        //TestResult t1 = SymbolicParameters.runSymbolicTests();
        //System.out.println(t1);
//
        //TestResult t2 = Lists.runSymbolicTests();
        //System.out.println(t2);

        TestResult t3 = SymbolicClasses.runSymbolicTests();
        System.out.println(t3);

        //TestResult t4 = new TestResult(t1, t2, t3);
        System.out.println(t3.toParsableString());
        System.out.println("✅ TESTS COMPLETED");
    }
}
