package de.uzl.its.tests;

import java.util.Random;

public class SomeClass {
    static int y;

    static {
        Random random = new Random();

        if (random.nextBoolean()) {
            y = 10;
        } else {
            y = 5;
        }
    }
}
