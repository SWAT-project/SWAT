package de.uzl.its.tests;

import de.uzl.its.tests.helper.*;

import java.util.List;

public class SymbolicClasses {
    public static int test_decl(DeclarationsDTO dto){
        System.out.println("[TEST] Testing DeclarationsDataClass");
        int i = dto.i;
        int j = dto.j;
        int k = dto.k;
        int l = DeclarationsDTO.l;
        int m = dto.getM();

        if(i == 42) return 1;
        if(j == 43) return 2;
        if(k == 44) return 3;
        if(l == 45) return 4;
        if(m == 46) return 5;
        return 0;
    }

    public static int test_primitives(PrimitiveDTO dto){
        System.out.println("[TEST] Testing PrimitiveDataClass");
        int i = dto.i;
        short s = dto.s;
        byte b = dto.b;
        char c = dto.c;
        long l = dto.l;
        float f = dto.f;
        double d = dto.d;
        boolean bool = dto.bool;

        if(i == 42) return 1;
        if(s == 1000) return 2;
        if(b == 42) return 3;
        if(c == 'A') return 4;
        if(l == 100000L) return 5;
        if(f == 3.14f) return 6;
        if(d == 2.718) return 7;
        if(bool) return 8;
        return 0;
    }

    public static int test_boxed_primitives(BoxedPrimitiveDTO dto){
        System.out.println("[TEST] Testing BoxedPrimitiveDataClass");
        Integer i = dto.i;
        Short s = dto.s;
        Byte b = dto.b;
        Character c = dto.c;
        Long l = dto.l;
        Float f = dto.f;
        Double d = dto.d;
        Boolean bool = dto.bool;

        if(i == 42) return 1;
        if(s == 1000) return 2;
        if(b == 42) return 3;
        if(c == 'A') return 4;
        if(l == 100000L) return 5;
        if(f == 3.14f) return 6;
        if(d == 2.718) return 7;
        if(bool) return 8;
        return 0;
    }

    public static int test_references(ReferenceDTO dto){
        String s = dto.s;
        String t = dto.t;
        List list = dto.list;
        List arrayList = dto.arrayList;
        List genericList = dto.genericList;

        NestedDTO ref = dto.ref;

        if(s.equals("foo")) return 1;
        if(t.equals("fool")) return 2;
        if(list.get(0) == "bar") return 3;
        if(list.get(1) == "bar") return 4;
        if(list.get(2) == "bar") return 5;
        if(arrayList.get(0) == "bar") return 6;
        if(arrayList.get(1) == "bar") return 7;
        if(genericList.get(0) == "bar") return 8;
        if(genericList.get(1) == "bar") return 9;
        if(ref.s.equals("bar")) return 6;
        return 0;
    }

    /**
     * Test runner that exercises each supported data type with symbolic variables
     */
    public static TestResult runSymbolicTests() {
        TestResult t = new TestResult("Symbolic Classes");

        // Test all primitive types
        System.out.println("Testing different variable declarations:");
        test_decl(new DeclarationsDTO()); t.totalTests += 4; t.symbolicVars += 4;

        test_primitives(new PrimitiveDTO()); t.totalTests += 8; t.symbolicVars += 8;
        test_boxed_primitives(new BoxedPrimitiveDTO()); t.totalTests += 8; t.symbolicVars += 8;
        test_references(new ReferenceDTO()); t.totalTests += 10; t.symbolicVars += 10;
        return t;

    }
}
