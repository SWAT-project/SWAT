package de.uzl.its.uninstrumented;
public class Helper extends AbstractHelper {
    public static int staticInt2 = 200;
    public static void main(String[] args) {
        Helper helper = new Helper();
        helper.doSomething();
    }

    public void doSomething() {
        System.out.println("Hello, World!");
    }
}