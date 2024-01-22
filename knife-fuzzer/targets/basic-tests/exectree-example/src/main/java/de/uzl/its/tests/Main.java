package de.uzl.its.tests;

public class Main {
    public static void main(String[] args) {
        System.out.println(ex(1));
        System.out.println(ex(20));
        System.out.println(ex(0));
    }

    public static int ex(int x) {
        int y = SomeClass.y;
        if (x < y) {
            try {
                return y / x;
            } catch (Exception e) {
                return -1;
            }
        } else {
            return x / y;
        }
    }
}
