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
        StringTest.runSymbolicTests();
    }
}
