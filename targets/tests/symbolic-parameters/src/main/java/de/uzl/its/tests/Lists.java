package de.uzl.its.tests;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Lists {
    public static int test_List(List<Integer> list){
        System.out.println("[TEST] Testing int symbolic variable");
        return list.get(0) == 42 ? 0 : 1;
    }
    public static int test_List_in_dto(HelperDataClass dto){
        System.out.println("[TEST] symbolic list in dto");
        return "foo".equals(dto.list.get(0)) ? 0 : 1;
    }



    /**
     * Test runner that exercises each supported data type with symbolic variables
     */
    public static TestResult runSymbolicTests() {
        TestResult t = new TestResult("List");
        // Test all primitive types
        System.out.println("Testing List");
        List<Integer> list = new ArrayList<>();
        list.add(-1);
        list.add(0);
        test_List(list); t.totalTests++; t.symbolicVars += 2;
        HelperDataClass dto = new HelperDataClass();
        test_List_in_dto(dto); t.totalTests++; t.symbolicVars += 2;
        return t;

    }

}
