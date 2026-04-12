package de.uzl.its.tests;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SymbolicParameters {

    public static int test_int(int i){
        System.out.println("[TEST] Testing int symbolic variable");
        return i == 42 ? 0 : 1;
    }
    public static int test_int_arr(int[] i){
        System.out.println("[TEST] Testing int array symbolic variable");
        return i[5] == 42 ? 0 : 1;
    }

    public static int test_Integer(Integer i){
        System.out.println("[TEST] Testing Integer symbolic variable");
        return i == 42 ? 0 : 1;
    }

    public static int test_boolean(boolean b){
        System.out.println("[TEST] Testing boolean symbolic variable");
        return b ? 0 : 1;
    }
    public static int test_boolean_arr(boolean[] b){
        System.out.println("[TEST] Testing boolean array symbolic variable");
        return b[5] ? 0 : 1;
    }

    public static int test_Boolean(Boolean b){
        System.out.println("[TEST] Testing Boolean symbolic variable");
        return b ? 0 : 1;
    }

    public static int test_byte(byte b){
        System.out.println("[TEST] Testing byte symbolic variable");
        return b == 42 ? 0 : 1;
    }
    public static int test_byte_arr(byte[] b){
        System.out.println("[TEST] Testing byte array symbolic variable");
        return b[5] == 42 ? 0 : 1;
    }

    public static int test_Byte(Byte b){
        System.out.println("[TEST] Testing Byte symbolic variable");
        return b == 42 ? 0 : 1;
    }

    public static int test_char(char c){
        System.out.println("[TEST] Testing char symbolic variable");
        return c == 'A' ? 0 : 1;
    }
    public static int test_char_arr(char[] c){
        System.out.println("[TEST] Testing char array symbolic variable");
        return c[5] == 'A' ? 0 : 1;
    }

    public static int test_Character(Character c){
        System.out.println("[TEST] Testing Character symbolic variable");
        return c == 'A' ? 0 : 1;
    }

    public static int test_short(short s){
        System.out.println("[TEST] Testing short symbolic variable");
        return s == 1000 ? 0 : 1;
    }
    public static int test_short_arr(short[] s){
        System.out.println("[TEST] Testing short array symbolic variable");
        return s[5] == 1000 ? 0 : 1;
    }

    public static int test_Short(Short s){
        System.out.println("[TEST] Testing Short symbolic variable");
        return s == 1000 ? 0 : 1;
    }

    public static int test_long(long l){
        System.out.println("[TEST] Testing long symbolic variable");
        return l == 123456789L ? 0 : 1;
    }
    public static int test_long_arr(long[] l){
        System.out.println("[TEST] Testing long array symbolic variable");
        return l[5] == 123456789L ? 0 : 1;
    }

    public static int test_Long(Long l){
        System.out.println("[TEST] Testing Long symbolic variable");
        return l == 123456789L ? 0 : 1;
    }

    public static int test_float(float f){
        System.out.println("[TEST] Testing float symbolic variable");
        return f == 3.14f ? 0 : 1;
    }
    public static int test_float_arr(float[] f){
        System.out.println("[TEST] Testing float array symbolic variable");
        return f[5] == 3.14f ? 0 : 1;
    }

    public static int test_Float(Float f){
        System.out.println("[TEST] Testing Float symbolic variable");
        return f == 3.14f ? 0 : 1;
    }

    public static int test_double(double d){
        System.out.println("[TEST] Testing double symbolic variable");
        return d == 2.71828 ? 0 : 1;
    }
    public static int test_double_arr(double[] d){
        System.out.println("[TEST] Testing double array symbolic variable");
        return d[5] == 2.71828 ? 0 : 1;
    }

    public static int test_Double(Double d){
        System.out.println("[TEST] Testing Double symbolic variable");
        return d == 2.71828 ? 0 : 1;
    }

    public static int test_String(String s){
        System.out.println("[TEST] Testing String symbolic variable");
        return s.equals("hello") ? 0 : 1;
    }
    public static int test_String_arr(String[] s){
        System.out.println("[TEST] Testing String array symbolic variable");
        return s[5].equals("hello") ? 0 : 1;
    }


    public static int test_arrayList(ArrayList<String> list){
        System.out.println("[TEST] Testing ArrayList symbolic variable");

        String elem = list.get(1);

        return elem.equals("secret") ? 0 : 1;
    }

    public static int test_list(List<String> list){
        System.out.println("[TEST] Testing List symbolic variable");

        String elem = list.get(0);

        return elem.equals("target") ? 0 : 1;
    }

    public static int test_linkedList(LinkedList<String> list){
        System.out.println("[TEST] Testing LinkedList symbolic variable");

        String elem = list.get(2);

        return elem.equals("needle") ? 0 : 1;
    }

    /**
     * Test runner that exercises each supported data type with symbolic variables
     */
    public static TestResult runSymbolicTests() {
        TestResult t = new TestResult("Primitive and Reference Types");
        
        // Test all primitive types
        System.out.println("Testing primitive types:");
        test_int(0); t.totalTests++; t.symbolicVars++;
        test_boolean(false); t.totalTests++; t.symbolicVars++;
        test_byte((byte)0); t.totalTests++; t.symbolicVars++;
        test_char('X'); t.totalTests++; t.symbolicVars++;
        test_short((short)0); t.totalTests++; t.symbolicVars++;
        test_long(0L); t.totalTests++; t.symbolicVars++;
        test_float(0.0f); t.totalTests++; t.symbolicVars++;
        test_double(0.0); t.totalTests++; t.symbolicVars++;

        // Test boxed primitive types
        System.out.println("\nTesting boxed primitive types:");
        test_Integer(0); t.totalTests++; t.symbolicVars++;
        test_Boolean(false); t.totalTests++; t.symbolicVars++;
        test_Byte((byte)0); t.totalTests++; t.symbolicVars++;
        test_Character('X'); t.totalTests++; t.symbolicVars++;
        test_Short((short)0); t.totalTests++; t.symbolicVars++;
        test_Long(0L); t.totalTests++; t.symbolicVars++;
        test_Float(0.0f); t.totalTests++; t.symbolicVars++;
        test_Double(0.0); t.totalTests++; t.symbolicVars++;

        // Test arrays
        System.out.println("\nTesting array of primitives:");
        test_int_arr(new int[]{0,1,2,3,4,0,6,7,8,9}); t.totalTests++; t.symbolicVars++;
        test_boolean_arr(new boolean[]{false,false,false,false,false,false,false,false,false,false}); t.totalTests++; t.symbolicVars++;
        test_byte_arr(new byte[]{0,1,2,3,4,0,6,7,8,9}); t.totalTests++; t.symbolicVars++;
        test_char_arr(new char[]{'X','X','X','X','X','X','X','X','X','X'}); t.totalTests++; t.symbolicVars++;
        test_short_arr(new short[]{0,1,2,3,4,0,6,7,8,9}); t.totalTests++; t.symbolicVars++;
        test_long_arr(new long[]{0L,1L,2L,3L,4L,0L,6L,7L,8L,9L}); t.totalTests++; t.symbolicVars++;
        test_float_arr(new float[]{0.0f,1.0f,2.0f,3.0f,4.0f,0.0f,6.0f,7.0f,8.0f,9.0f}); t.totalTests++; t.symbolicVars++;
        test_double_arr(new double[]{0.0,1.0,2.0,3.0,4.0,0.0,6.0,7.0,8.0,9.0}); t.totalTests++; t.symbolicVars++;
        test_String_arr(new String[]{"a","b","c","d","e","f","g","h","i","j"}); t.totalTests++; t.symbolicVars++;

        // Test String
        System.out.println("\nTesting String:");
        test_String("world"); t.totalTests++; t.symbolicVars++;

        System.out.println("\nTesting reference types:");
        
        // Test ArrayList
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("foo");
        arrayList.add("bar");
        arrayList.add("baz");
        test_arrayList(arrayList); t.totalTests++;
        t.symbolicVars += 3; // Track after test execution
        
        // Test List interface with ArrayList implementation
        List<String> list = new ArrayList<>();
        list.add("alpha");
        list.add("beta");
        list.add("gamma");
        test_list(list); t.totalTests++;
        t.symbolicVars += 3; // Track after test execution
        
        // Test LinkedList
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("first");
        linkedList.add("second");
        linkedList.add("third");
        test_linkedList(linkedList); t.totalTests++;
        t.symbolicVars += 3; // Track after test execution
        return t;

    }
}