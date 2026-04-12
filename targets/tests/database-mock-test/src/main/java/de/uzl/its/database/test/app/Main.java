package de.uzl.its.database.test.app;

public class Main {
    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        test("France");
    }

    public static Country helper(String name) {
        return (new CountryRepository()).getCountry(name);
    }

    public static Country test(String name) {
        return helper(name);
    }
}