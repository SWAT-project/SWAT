package de.uzl.its.tests;

public class TypeChange1 {

    /**
     * Test function. Parameter is made symbolic
     *
     * @param param
     */
    private void test(String param) {

        String[] parts = param.split("/");

        if (parts[0].equals("ressource2")) {
            System.out.println("Path 2");
        }
    }

    public static void main(String[] args) {

        new TypeChange1().test("ressource1/1");
    }
}
