package de.uzl.its.tests;

import de.uzl.its.swat.annotations.Symbolic;

public class Main {
    public static void main(String[] args) {
        @Symbolic
        String input = "hey";

        // assert input.toLowerCase() != "bad";
        assert input != "bad";
    }
}
