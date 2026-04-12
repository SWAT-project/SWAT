package de.uzl.its.tests.exclude;

import de.uzl.its.tests.include.UtilIncl;

public class UtilExcl {
    private static int CONSTANT = 42;

    public static int constant() {
        System.out.println("[SUT::UtilExcl] Retrieving constant");
        return CONSTANT;
    }

    public static int constant2() {
        System.out.println("[SUT::UtilExcl] Retrieving constant");
        return UtilIncl.CONSTANT;
    }

    public static int constant3() {
        System.out.println("[SUT::UtilExcl] Retrieving constant");
        return UtilIncl.getCONSTANT();
    }

    public static int constant4(int idx) {
        System.out.println("[SUT::UtilExcl] Retrieving constant");
        return idx == 0 ? CONSTANT : -1;
    }

    public static int constant(boolean flag, int alternative) {
        System.out.println("[SUT::UtilExcl] Retrieving constant");
        return flag ? CONSTANT : alternative;
    }
}
