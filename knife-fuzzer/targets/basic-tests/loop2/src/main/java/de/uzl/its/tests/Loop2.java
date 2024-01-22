package de.uzl.its.tests;

import java.util.ArrayList;

public class Loop2 {

    /**
     * Test function. Parameter is made symbolic
     *
     * @param param
     */
    private void test(int param) {

        ArrayList<Integer> db = new ArrayList<>();
        db.add(100);
        db.add(200);
        db.add(333);

        for (int v : db) {
            if (v == param) {
                System.out.println("Found a case for id = " + v);
                return;
            }
        }

        System.out.println("Did not find a case");
    }

    public static void main(String[] args) {

        new Loop2().test(1);
    }
}
