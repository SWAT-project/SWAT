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


        // Test Integer autoboxing.
        Integer i1 = Integer.valueOf(100);
        Integer i2 = Integer.valueOf(100);
        System.out.println("Integer i1 == i2: " + (i1 == i2));  // Expect false.
        assert i1 != i2;
        // Testing an out-of-cache value (for clarity).
        Integer i3 = Integer.valueOf(200);
        Integer i4 = Integer.valueOf(200);
        System.out.println("Integer i3 == i4: " + (i3 == i4));  // Expect false.
        assert i3 != i4;
        // Test Long autoboxing.
        Long l1 = Long.valueOf(100L);
        Long l2 = Long.valueOf(100L);
        System.out.println("Long l1 == l2: " + (l1 == l2));  // Expect false.
        assert l1 != l2;
        Long l3 = Long.valueOf(200L);
        Long l4 = Long.valueOf(200L);
        System.out.println("Long l3 == l4: " + (l3 == l4));  // Expect false.
        assert l3 != l4;
        // Test Short autoboxing.
        Short sh1 = Short.valueOf((short)100);
        Short sh2 = Short.valueOf((short)100);
        System.out.println("Short sh1 == sh2: " + (sh1 == sh2));  // Expect false.
        assert sh1 != sh2;
        // Test Byte autoboxing.
        Byte b1 = Byte.valueOf((byte)10);
        Byte b2 = Byte.valueOf((byte)10);
        System.out.println("Byte b1 == b2: " + (b1 == b2));  // Expect false.
        assert b1 != b2;
        // Test Character autoboxing.
        Character c1 = Character.valueOf('A');
        Character c2 = Character.valueOf('A');
        System.out.println("Character c1 == c2: " + (c1 == c2));  // Expect false.
        assert c1 != c2;
        // Test Boolean autoboxing.
        Boolean bool1 = Boolean.valueOf(true);
        Boolean bool2 = Boolean.valueOf(true);
        System.out.println("Boolean bool1 == bool2: " + (bool1 == bool2));  // Expect false.
        assert bool1 != bool2;
    }
}
