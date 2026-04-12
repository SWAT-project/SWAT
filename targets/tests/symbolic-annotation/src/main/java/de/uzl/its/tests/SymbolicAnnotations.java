package de.uzl.its.tests;

import de.uzl.its.swat.annotations.Symbolic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SymbolicAnnotations {

    public static int test_int_0(){
        System.out.println("[TEST] Testing int symbolic variable");
        @Symbolic int i = 0; // This is a symbolic variable
        return i == 42 ? 0 : 1;
    }

    public static int test_int_1(@Symbolic int i){
        System.out.println("[TEST] Testing int symbolic variable");
        return i == 42 ? 0 : 1;
    }

    public static int test_int_2(@Symbolic int i, int j){
        System.out.println("[TEST] Testing int symbolic variable");
        return i == j ? 0 : 1;
    }

    public static int test_int_3(int i, @Symbolic int j){
        System.out.println("[TEST] Testing int symbolic variable");
        return i == j ? 0 : 1;
    }

    public static int test_int_4(@Symbolic int i, @Symbolic int j){
        System.out.println("[TEST] Testing int symbolic variable");
        return i == j ? 0 : 1;
    }

    public static int test_int_5(){
        System.out.println("[TEST] Testing int symbolic variable");
        return Helper.k == 42 ? 0 : 1;
    }

    public int test_int_6(){
        System.out.println("[TEST] Testing int symbolic variable");
        Helper helper = new Helper();
        return helper.l == 42 ? 0 : 1;
    }

    public static int test_Integer(@Symbolic Integer i){
        System.out.println("[TEST] Testing Integer symbolic variable");
        return i == 42 ? 0 : 1;
    }

    public static int test_boolean(@Symbolic boolean b){
        System.out.println("[TEST] Testing boolean symbolic variable");
        return b ? 0 : 1;
    }

    public static int test_Boolean(@Symbolic Boolean b){
        System.out.println("[TEST] Testing Boolean symbolic variable");
        return b ? 0 : 1;
    }

    public static int test_byte(@Symbolic byte b){
        System.out.println("[TEST] Testing byte symbolic variable");
        return b == 42 ? 0 : 1;
    }

    public static int test_Byte(@Symbolic Byte b){
        System.out.println("[TEST] Testing Byte symbolic variable");
        return b == 42 ? 0 : 1;
    }

    public static int test_char(@Symbolic char c){
        System.out.println("[TEST] Testing char symbolic variable");
        return c == 'A' ? 0 : 1;
    }

    public static int test_Character(@Symbolic Character c){
        System.out.println("[TEST] Testing Character symbolic variable");
        return c == 'A' ? 0 : 1;
    }

    public static int test_short(@Symbolic short s){
        System.out.println("[TEST] Testing short symbolic variable");
        return s == 1000 ? 0 : 1;
    }

    public static int test_Short(@Symbolic Short s){
        System.out.println("[TEST] Testing Short symbolic variable");
        return s == 1000 ? 0 : 1;
    }

    public static int test_long(@Symbolic long l){
        System.out.println("[TEST] Testing long symbolic variable");
        return l == 123456789L ? 0 : 1;
    }

    public static int test_Long(@Symbolic Long l){
        System.out.println("[TEST] Testing Long symbolic variable");
        return l == 123456789L ? 0 : 1;
    }

    public static int test_float(@Symbolic float f){
        System.out.println("[TEST] Testing float symbolic variable");
        return f == 3.14f ? 0 : 1;
    }

    public static int test_Float(@Symbolic Float f){
        System.out.println("[TEST] Testing Float symbolic variable");
        return f == 3.14f ? 0 : 1;
    }

    public static int test_double(@Symbolic double d){
        System.out.println("[TEST] Testing double symbolic variable");
        return d == 2.71828 ? 0 : 1;
    }

    public static int test_Double(@Symbolic Double d){
        System.out.println("[TEST] Testing Double symbolic variable");
        return d == 2.71828 ? 0 : 1;
    }

    public static int test_String(@Symbolic String s){
        System.out.println("[TEST] Testing String symbolic variable");
        return s.equals("hello") ? 0 : 1;
    }


    public static int test_arrayList(@Symbolic ArrayList<String> list){
        System.out.println("[TEST] Testing ArrayList symbolic variable");

        String elem = list.get(1);

        return elem.equals("secret") ? 0 : 1;
    }

    public static int test_list(@Symbolic List<String> list){
        System.out.println("[TEST] Testing List symbolic variable");

        String elem = list.get(0);

        return elem.equals("target") ? 0 : 1;
    }

    public static int test_linkedList(@Symbolic LinkedList<String> list){
        System.out.println("[TEST] Testing LinkedList symbolic variable");

        String elem = list.get(2);

        return elem.equals("needle") ? 0 : 1;
    }

    /**
     * Test runner that exercises each supported data type with symbolic variables
     */
    public static void runSymbolicTests() {
        SymbolicAnnotations sa = new SymbolicAnnotations();
        int totalTests = 0;
        int symbolicVars = 0;

        // Test all primitive types
        System.out.println("Testing primitive types:");
        test_int_0(); totalTests++; symbolicVars++;
        test_int_1(0); totalTests++; symbolicVars++;
        test_int_2(0,1); totalTests++; symbolicVars++;
        test_int_3(0,1); totalTests++; symbolicVars++;
        test_int_4(0,1); totalTests++; symbolicVars+=2;
        // test_int_5(); totalTests++; symbolicVars++; // Todo: This test does not work yet
        sa.test_int_6(); totalTests++; symbolicVars++;
        test_boolean(false); totalTests++; symbolicVars++;
        test_byte((byte)0); totalTests++; symbolicVars++;
        test_char('X'); totalTests++; symbolicVars++;
        test_short((short)0); totalTests++; symbolicVars++;
        test_long(0L); totalTests++; symbolicVars++;
        test_float(0.0f); totalTests++; symbolicVars++;
        test_double(0.0); totalTests++; symbolicVars++;

        // Test boxed primitive types
        System.out.println("\nTesting boxed primitive types:");
        test_Integer(0); totalTests++; symbolicVars++;
        test_Boolean(false); totalTests++; symbolicVars++;
        test_Byte((byte)0); totalTests++; symbolicVars++;
        test_Character('X'); totalTests++; symbolicVars++;
        test_Short((short)0); totalTests++; symbolicVars++;
        test_Long(0L); totalTests++; symbolicVars++;
        test_Float(0.0f); totalTests++; symbolicVars++;
        test_Double(0.0); totalTests++; symbolicVars++;

        // Test String
        System.out.println("\nTesting String:");
        test_String("world"); totalTests++; symbolicVars++;

        System.out.println("\nTesting reference types:");

        // Test ArrayList
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("foo");
        arrayList.add("bar");
        arrayList.add("baz");
        test_arrayList(arrayList); totalTests++;
        symbolicVars += 3; // Track after test execution

        // Test List interface with ArrayList implementation
        List<String> list = new ArrayList<>();
        list.add("alpha");
        list.add("beta");
        list.add("gamma");
        test_list(list); totalTests++;
        symbolicVars += 3; // Track after test execution

        // Test LinkedList
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("first");
        linkedList.add("second");
        linkedList.add("third");
        test_linkedList(linkedList); totalTests++;
        symbolicVars += 3; // Track after test execution

        System.out.println("\n=== Test Summary ===");
        System.out.println("Executed tests: " + totalTests);
        System.out.println("Expected symbolic variables: " + symbolicVars);
        System.out.println();

        System.out.println("✅ SYMBOLIC LIFTING TESTS COMPLETED");

    }
}