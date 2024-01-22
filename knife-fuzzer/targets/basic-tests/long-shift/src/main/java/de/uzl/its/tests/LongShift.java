package de.uzl.its.tests;

public class LongShift {

    /**
     * An external function that does a simple print. It might prevent method inlining for some
     * tests. Make sure your test functions is not inlied by checkiing with 'javap -c -private
     * StringIf.class'
     *
     * @param str String to be printed
     */
    private void print(String str) {
        System.out.println(str);
    }

    /**
     * Test function. Parameter is made symbolic
     *
     * @param param
     */
    private void test(long param) {
        param = param << 2;
        param = param >> 2;
        param = param >>> 2;
        if (param == 10L) {
            print("path_0");
        } else {
            print("path_1");
        }
    }

    public static void main(String[] args) {

        new LongShift().test(10L);
    }
}
