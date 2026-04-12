package de.uzl.its.tests.arrays;

import de.uzl.its.swat.annotations.Symbolic;

/**
 * Simple test for array symbolization with explorer integration.
 * This test demonstrates that arrays can be made symbolic using annotations
 * and that the explorer can generate new array values to explore different paths.
 */
public class SimpleArrayTest {

    /**
     * Simple test with an int array that has 3 distinct paths:
     * 1. arr[0] == 42 && arr[1] == 10  -> returns 1
     * 2. arr[0] > 100                   -> returns 2
     * 3. default                        -> returns 0
     */
    public static int testIntArray(@Symbolic int[] arr) {
        System.out.println("[TEST] Testing int array symbolization");
        System.out.println("[TEST] Array length: " + arr.length);

        if (arr.length < 2) {
            return -1;
        }

        System.out.println("[TEST] arr[0] = " + arr[0] + ", arr[1] = " + arr[1]);

        // Path 1: specific values
        if (arr[0] == 42 && arr[1] == 10) {
            System.out.println("[TEST] Found path 1: arr[0]==42 && arr[1]==10");
            return 1;
        }

        // Path 2: large value
        if (arr[0] > 100) {
            System.out.println("[TEST] Found path 2: arr[0]>100");
            return 2;
        }

        // Path 3: default
        System.out.println("[TEST] Found path 3: default");
        return 0;
    }

    public static void main(String[] args) {
        // Initial call with default array
        int[] testArray = new int[]{0, 0};
        int result = testIntArray(testArray);
        System.out.println("[RESULT] Returned: " + result);
    }
}
