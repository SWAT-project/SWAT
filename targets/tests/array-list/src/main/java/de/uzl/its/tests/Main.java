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
        @Symbolic String s1 = "Hello, World!";
        String s2 = "Hello, World!";
        String s3 = "Unique String";
        ArrayList<String> list = new ArrayList<>();
        list.add(s1);
        list.add(s2);
        list.add(s3);

        String t1 = list.get(0);
        String t2 = list.get(2);
        if(!t1.equals(t2)) {
            System.out.println("t1 != t2");
        } else {
            System.out.println("t1 == t2");
        };

    }
}
