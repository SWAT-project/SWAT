package de.uzl.its.tests.arrays;

/**
 * Comprehensive tests for primitive array symbolization.
 * Tests cover:
 * - All primitive array types (boolean[], byte[], char[], short[], int[], long[], float[], double[])
 * - Symbolic array access patterns
 * - Reasoning on array length
 * - Array element comparisons
 * - Multi-dimensional arrays
 */
public class PrimitiveArrayTests {

    // ==================== BOOLEAN ARRAY TESTS ====================

    /**
     * Test boolean array with symbolic values
     * Tests: array access, boolean comparisons, multiple branches
     */
    public static int test_boolean_array_values(boolean[] arr) {
        System.out.println("[TEST] Testing boolean[] with symbolic values");

        if (arr.length < 3) return -1;

        // Test individual element comparisons
        if (arr[0] == true && arr[1] == false) return 1;
        if (arr[1] == true && arr[2] == false) return 2;
        if (arr[0] && arr[1] && arr[2]) return 3;
        if (!arr[0] && !arr[1]) return 4;

        return 0;
    }

    /**
     * Test boolean array length reasoning
     */
    public static int test_boolean_array_length(boolean[] arr) {
        System.out.println("[TEST] Testing boolean[] length reasoning");

        if (arr.length == 0) return 1;
        if (arr.length == 1) return 2;
        if (arr.length == 5) return 3;
        if (arr.length > 10) return 4;
        if (arr.length < 3) return 5;

        return 0;
    }

    // ==================== BYTE ARRAY TESTS ====================

    /**
     * Test byte array with symbolic values
     * Tests: byte comparisons, boundary values, arithmetic
     */
    public static int test_byte_array_values(byte[] arr) {
        System.out.println("[TEST] Testing byte[] with symbolic values");

        if (arr.length < 4) return -1;

        // Test specific values
        if (arr[0] == 42) return 1;
        if (arr[1] == 127) return 2;  // Byte.MAX_VALUE
        if (arr[2] == -128) return 3; // Byte.MIN_VALUE
        if (arr[3] == 0) return 4;

        // Test arithmetic operations
        if (arr[0] + arr[1] == 100) return 5;
        if (arr[0] > arr[1]) return 6;

        return 0;
    }

    /**
     * Test byte array length and range checks
     */
    public static int test_byte_array_length(byte[] arr) {
        System.out.println("[TEST] Testing byte[] length reasoning");

        if (arr.length == 0) return 1;
        if (arr.length >= 10) return 2;
        if (arr.length < 5 && arr.length > 0) {
            if (arr[0] > 50) return 3;
        }

        return 0;
    }

    /**
     * Test byte array with bounds checking
     */
    public static int test_byte_array_bounds(byte[] arr) {
        System.out.println("[TEST] Testing byte[] bounds checking");

        if (arr.length == 0) return 1;

        // Access first element
        if (arr[0] == 127) return 2;

        // Access middle element if array is large enough
        if (arr.length >= 10) {
            if (arr[5] == 42) return 3;
        }

        // Access last element
        if (arr.length > 0 && arr[arr.length - 1] == -128) return 4;

        return 0;
    }

    // ==================== CHAR ARRAY TESTS ====================

    /**
     * Test char array with symbolic values
     * Tests: character comparisons, ranges, special characters
     */
    public static int test_char_array_values(char[] arr) {
        System.out.println("[TEST] Testing char[] with symbolic values");

        if (arr.length < 5) return -1;

        // Test specific characters
        if (arr[0] == 'A') return 1;
        if (arr[1] == 'Z') return 2;
        if (arr[2] == '0') return 3;
        if (arr[3] == '9') return 4;
        if (arr[4] == ' ') return 5;

        // Test character ranges
        if (arr[0] >= 'a' && arr[0] <= 'z') return 6;
        if (arr[1] >= 'A' && arr[1] <= 'Z') return 7;
        if (arr[2] >= '0' && arr[2] <= '9') return 8;

        return 0;
    }

    /**
     * Test char array with string-like operations
     */
    public static int test_char_array_string_ops(char[] arr) {
        System.out.println("[TEST] Testing char[] string operations");

        if (arr.length < 3) return -1;

        // Test sequence matching
        if (arr[0] == 'f' && arr[1] == 'o' && arr[2] == 'o') return 1;
        if (arr[0] == 'b' && arr[1] == 'a' && arr[2] == 'r') return 2;

        return 0;
    }

    /**
     * Test char array with pattern matching
     */
    public static int test_char_array_patterns(char[] arr) {
        System.out.println("[TEST] Testing char[] pattern matching");

        if (arr.length < 4) return -1;

        // Test palindrome check for first 4 characters
        if (arr[0] == arr[3] && arr[1] == arr[2]) return 1;

        // Test all uppercase
        boolean allUpper = true;
        for (int i = 0; i < arr.length && i < 10; i++) {
            if (arr[i] < 'A' || arr[i] > 'Z') {
                allUpper = false;
                break;
            }
        }
        if (allUpper) return 2;

        return 0;
    }

    // ==================== SHORT ARRAY TESTS ====================

    /**
     * Test short array with symbolic values
     * Tests: short comparisons, boundary values
     */
    public static int test_short_array_values(short[] arr) {
        System.out.println("[TEST] Testing short[] with symbolic values");

        if (arr.length < 4) return -1;

        // Test specific values
        if (arr[0] == 1000) return 1;
        if (arr[1] == 32767) return 2;  // Short.MAX_VALUE
        if (arr[2] == -32768) return 3; // Short.MIN_VALUE
        if (arr[3] == 0) return 4;

        // Test comparisons
        if (arr[0] > 500 && arr[0] < 1500) return 5;
        if (arr[1] < arr[2]) return 6;

        return 0;
    }

    // ==================== INT ARRAY TESTS ====================

    /**
     * Test int array with symbolic values
     * Tests: integer comparisons, arithmetic, complex conditions
     */
    public static int test_int_array_values(int[] arr) {
        System.out.println("[TEST] Testing int[] with symbolic values");

        if (arr.length < 5) return -1;

        // Test specific values
        if (arr[0] == 42) return 1;
        if (arr[1] == 100) return 2;
        if (arr[2] == -999) return 3;

        // Test arithmetic operations
        if (arr[3] + arr[4] == 100) return 4;
        if (arr[0] * arr[1] == 4200) return 5;
        if (arr[1] - arr[0] == 58) return 6;

        // Test comparisons
        if (arr[0] > arr[1]) return 7;
        if (arr[2] < 0 && arr[3] > 0) return 8;

        return 0;
    }

    /**
     * Test int array with complex length reasoning
     */
    public static int test_int_array_length_complex(int[] arr) {
        System.out.println("[TEST] Testing int[] complex length reasoning");

        if (arr.length == 0) return 1;
        if (arr.length == 1 && arr[0] == 42) return 2;
        if (arr.length == 2 && arr[0] + arr[1] == 100) return 3;
        if (arr.length >= 3 && arr[0] == arr[1] && arr[1] == arr[2]) return 4;
        if (arr.length > 5 && arr[arr.length - 1] == 999) return 5;

        return 0;
    }

    /**
     * Test int array with element iteration patterns
     */
    public static int test_int_array_iteration(int[] arr) {
        System.out.println("[TEST] Testing int[] iteration patterns");

        if (arr.length < 3) return -1;

        int sum = 0;
        for (int i = 0; i < arr.length && i < 3; i++) {
            sum += arr[i];
        }

        if (sum == 100) return 1;
        if (sum > 200) return 2;
        if (sum < 0) return 3;

        // Test all elements equal
        boolean allEqual = true;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] != arr[0]) {
                allEqual = false;
                break;
            }
        }
        if (allEqual && arr[0] == 42) return 4;

        return 0;
    }

    /**
     * Test int array with index-dependent operations
     */
    public static int test_int_array_index_dependent(int[] arr) {
        System.out.println("[TEST] Testing int[] with index-dependent operations");

        if (arr.length < 5) return -1;

        // Test if array is sorted ascending
        boolean sorted = true;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < arr[i - 1]) {
                sorted = false;
                break;
            }
        }
        if (sorted && arr[0] == 1) return 1;

        // Test if array contains specific sequence
        if (arr[0] == 10 && arr[1] == 20 && arr[2] == 30) return 2;

        // Test alternating pattern
        if (arr[0] == 0 && arr[1] == 1 && arr[2] == 0 && arr[3] == 1) return 3;

        return 0;
    }

    // ==================== LONG ARRAY TESTS ====================

    /**
     * Test long array with symbolic values
     * Tests: long comparisons, large values
     */
    public static int test_long_array_values(long[] arr) {
        System.out.println("[TEST] Testing long[] with symbolic values");

        if (arr.length < 4) return -1;

        // Test specific values
        if (arr[0] == 123456789L) return 1;
        if (arr[1] == 9223372036854775807L) return 2;  // Long.MAX_VALUE
        if (arr[2] == -9223372036854775808L) return 3; // Long.MIN_VALUE
        if (arr[3] == 0L) return 4;

        // Test arithmetic
        if (arr[0] > 100000000L && arr[0] < 200000000L) return 5;

        return 0;
    }

    // ==================== FLOAT ARRAY TESTS ====================

    /**
     * Test float array with symbolic values
     * Tests: floating point comparisons, ranges
     */
    public static int test_float_array_values(float[] arr) {
        System.out.println("[TEST] Testing float[] with symbolic values");

        if (arr.length < 5) return -1;

        // Test specific values
        if (arr[0] == 3.14f) return 1;
        if (arr[1] == 2.71f) return 2;
        if (arr[2] == 0.0f) return 3;
        if (arr[3] == -1.5f) return 4;

        // Test ranges
        if (arr[4] > 10.0f && arr[4] < 20.0f) return 5;
        if (arr[0] > arr[1]) return 6;

        // Test arithmetic
        if (arr[0] + arr[1] > 5.0f) return 7;

        return 0;
    }

    // ==================== DOUBLE ARRAY TESTS ====================

    /**
     * Test double array with symbolic values
     * Tests: double precision comparisons, arithmetic
     */
    public static int test_double_array_values(double[] arr) {
        System.out.println("[TEST] Testing double[] with symbolic values");

        if (arr.length < 5) return -1;

        // Test specific values
        if (arr[0] == 2.71828) return 1;
        if (arr[1] == 3.14159) return 2;
        if (arr[2] == 0.0) return 3;
        if (arr[3] == -99.99) return 4;

        // Test ranges
        if (arr[4] < -100.0) return 5;
        if (arr[0] > 2.0 && arr[0] < 3.0) return 6;

        // Test comparisons between elements
        if (arr[0] < arr[1]) return 7;
        if (arr[2] == arr[3] + 99.99) return 8;

        return 0;
    }

    // ==================== MULTI-DIMENSIONAL ARRAY TESTS ====================

    /**
     * Test 2D int array (multidimensional)
     * Tests: nested array access, 2D reasoning
     */
    public static int test_2d_int_array(int[][] arr) {
        System.out.println("[TEST] Testing int[][] (2D array)");

        if (arr.length < 2) return -1;
        if (arr[0].length < 2) return -2;
        if (arr[1].length < 2) return -3;

        // Test specific positions
        if (arr[0][0] == 1) return 1;
        if (arr[0][1] == 2) return 2;
        if (arr[1][0] == 3) return 3;
        if (arr[1][1] == 4) return 4;

        // Test diagonal
        if (arr[0][0] == arr[1][1]) return 5;

        // Test sum
        if (arr[0][0] + arr[0][1] + arr[1][0] + arr[1][1] == 10) return 6;

        return 0;
    }

    /**
     * Test 3D int array
     * Tests: deeply nested array access
     */
    public static int test_3d_int_array(int[][][] arr) {
        System.out.println("[TEST] Testing int[][][] (3D array)");

        if (arr.length < 2) return -1;
        if (arr[0].length < 2) return -2;
        if (arr[0][0].length < 2) return -3;

        if (arr[0][0][0] == 1) return 1;
        if (arr[0][0][1] == 2) return 2;
        if (arr[0][1][0] == 3) return 3;
        if (arr[1][0][0] == 4) return 4;

        return 0;
    }

    /**
     * Test 2D array with variable dimensions
     */
    public static int test_2d_array_variable_dimensions(int[][] arr) {
        System.out.println("[TEST] Testing int[][] with variable dimensions");

        if (arr.length == 0) return 1;
        if (arr.length == 1 && arr[0].length == 0) return 2;
        if (arr.length == 2) {
            if (arr[0].length != arr[1].length) return 3;
            if (arr[0].length == 3 && arr[0][0] == arr[0][1] && arr[0][1] == arr[0][2]) return 4;
        }

        return 0;
    }

    /**
     * Test runner that exercises all array test methods
     */
    public static TestResult runSymbolicTests() {
        TestResult t = new TestResult("Primitive Array Symbolization Tests");

        // Boolean array tests
        System.out.println("\n=== BOOLEAN ARRAY TESTS ===");
        test_boolean_array_values(new boolean[]{false, true, false});
        t.totalTests += 4; t.symbolicVars += 3;

        test_boolean_array_length(new boolean[]{false, true, false, true, false});
        t.totalTests += 5; t.symbolicVars += 1; // Length is symbolic

        // Byte array tests
        System.out.println("\n=== BYTE ARRAY TESTS ===");
        test_byte_array_values(new byte[]{0, 1, 2, 3});
        t.totalTests += 6; t.symbolicVars += 4;

        test_byte_array_length(new byte[]{0, 1, 2});
        t.totalTests += 3; t.symbolicVars += 1;

        test_byte_array_bounds(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        t.totalTests += 4; t.symbolicVars += 11;

        // Char array tests
        System.out.println("\n=== CHAR ARRAY TESTS ===");
        test_char_array_values(new char[]{'X', 'Y', '1', '2', 'Z'});
        t.totalTests += 8; t.symbolicVars += 5;

        test_char_array_string_ops(new char[]{'a', 'b', 'c'});
        t.totalTests += 2; t.symbolicVars += 3;

        test_char_array_patterns(new char[]{'a', 'b', 'b', 'a'});
        t.totalTests += 2; t.symbolicVars += 4;

        // Short array tests
        System.out.println("\n=== SHORT ARRAY TESTS ===");
        test_short_array_values(new short[]{0, 1, 2, 3});
        t.totalTests += 6; t.symbolicVars += 4;

        // Int array tests
        System.out.println("\n=== INT ARRAY TESTS ===");
        test_int_array_values(new int[]{0, 0, 0, 50, 50});
        t.totalTests += 8; t.symbolicVars += 5;

        test_int_array_length_complex(new int[]{0, 0, 0, 0, 0, 0});
        t.totalTests += 5; t.symbolicVars += 7; // Length + elements

        test_int_array_iteration(new int[]{10, 20, 30, 40});
        t.totalTests += 4; t.symbolicVars += 4;

        test_int_array_index_dependent(new int[]{0, 1, 2, 3, 4});
        t.totalTests += 3; t.symbolicVars += 5;

        // Long array tests
        System.out.println("\n=== LONG ARRAY TESTS ===");
        test_long_array_values(new long[]{0L, 1L, 2L, 3L});
        t.totalTests += 5; t.symbolicVars += 4;

        // Float array tests
        System.out.println("\n=== FLOAT ARRAY TESTS ===");
        test_float_array_values(new float[]{0.0f, 1.0f, 2.0f, 3.0f, 4.0f});
        t.totalTests += 7; t.symbolicVars += 5;

        // Double array tests
        System.out.println("\n=== DOUBLE ARRAY TESTS ===");
        test_double_array_values(new double[]{0.0, 1.0, 2.0, 3.0, 4.0});
        t.totalTests += 8; t.symbolicVars += 5;

        // Multi-dimensional array tests
        System.out.println("\n=== MULTI-DIMENSIONAL ARRAY TESTS ===");
        test_2d_int_array(new int[][]{{0, 0}, {0, 0}});
        t.totalTests += 6; t.symbolicVars += 4;

        test_3d_int_array(new int[][][]{{{0, 0}, {0, 0}}, {{0, 0}, {0, 0}}});
        t.totalTests += 4; t.symbolicVars += 8;

        test_2d_array_variable_dimensions(new int[][]{{1, 2, 3}, {4, 5, 6}});
        t.totalTests += 4; t.symbolicVars += 8; // Including dimension info

        return t;
    }
}
