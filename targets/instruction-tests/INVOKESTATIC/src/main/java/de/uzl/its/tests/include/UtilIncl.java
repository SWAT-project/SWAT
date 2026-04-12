package de.uzl.its.tests.include;

public class UtilIncl {
    public static int CONSTANT = 101;

    public static int getCONSTANT() {
        System.out.println("[SUT::UtilIncl] Retrieving constant");
        return CONSTANT;
    }
}
