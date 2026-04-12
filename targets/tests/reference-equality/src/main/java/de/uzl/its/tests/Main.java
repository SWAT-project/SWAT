package de.uzl.its.tests;

public class Main {


    public static void main(String[] args) {
        Integer i = 1;
        Integer j = 1;
        System.out.println("Integer 1 == 1: " + test_SymbolicDeinternedInteger(i, j));
        Integer k = new Integer(1);
        Integer l = new Integer(1);
        System.out.println("Symbolic Integer i = 1; Symbolic Integer j = 1; i == j: " + test_SymbolicDeinternedInteger(k, l));

        System.out.println("Integer i = 1; Integer j = 1; i == j: " + test_DeinternedInteger());
        HelperClass h1 = new HelperClass(1);
        HelperClass h2 = new HelperClass(1);
        System.out.println("new HelperClass(1) == new HelperClass(1): " + (h1 == h2));

        HelperClass h3 = new HelperClass(1);
        HelperClass h4 = new HelperClass(2);
        System.out.println("new HelperClass(1) == new HelperClass(2): " + (h3 == h4));
    }

    public static boolean test_SymbolicDeinternedInteger(Integer a, Integer b){
        return a == b;
    }
    public static boolean test_DeinternedInteger(){
        Integer i = 1;
        Integer j = 1;
        return i == j;
    }

    public static boolean test_SymbolicHelperClass(HelperClass a, HelperClass b){
        return a == b;
    }
}
