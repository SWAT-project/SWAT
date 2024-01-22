package de.uzl.its.tests;

public class Loop {

    /**
     * Test function. Parameter is made symbolic
     *
     * @param param
     */
    private void test(int param) {

        int i = 0;
        for (int iter = 0; iter < param; iter++) {
            i = iter;
        }
        if (i == 42) {
            System.out.println("Loop iterations were considered in formula!");
        }
    }

    public static void main(String[] args) {

        new Loop().test(1);
    }
}
