package de.uzl.its.tests;

import de.uzl.its.swat.annotations.Symbolic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StringTest {

    public static int test_equals(String s){
        System.out.println("[TEST] Testing String::equals");
        return s.equals("hello") ? 0 : 1;
    }
    public static int test_equalsIgnoreCase(String s){
        System.out.println("[TEST] Testing String::equals");
        return s.equalsIgnoreCase("hello") ? 0 : 1;
    }
    public static int test_equalsIgnoreCase2(String s){
        System.out.println("[TEST] Testing String::equals");

        return s.startsWith("HE") && s.equalsIgnoreCase("hello") ? 0 : 1;
    }
    /**
     * Test runner that exercises each supported data type with symbolic variables
     */
    public static void runSymbolicTests() {
        int totalTests = 0;
        
        // Test all primitive types
        System.out.println("Testing instance methods:");
        test_equals("foo"); totalTests++;
        test_equalsIgnoreCase("foo"); totalTests++;
        test_equalsIgnoreCase2("HEfoo"); totalTests++;

        System.out.println("\n=== Test Summary ===");
        System.out.println("Executed tests: " + totalTests);
        System.out.println();
        
        System.out.println("✅ SYMBOLIC LIFTING TESTS COMPLETED");

    }
}