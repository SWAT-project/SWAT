package de.uzl.its.targets;

import com.google.gson.Gson;

public class Harness {

    /**
     * Main function - acts as the harness for the symbolic execution It is important, that no
     * calculations on the values are done in this function
     *
     * @param args command line arguments - symbolic
     */
    public static void main(String[] args) {
        String symbolicString = args[0];
        toJsonString(symbolicString);
        // ensure the ordering of the parameters is identical to the args array
    }

    public static void toJsonString(String symbolicString) {
        Gson gson = new Gson();
        String res = gson.toJson(symbolicString);
        System.out.println(res);
        if(symbolicString == "y") System.out.println(res);
        else System.out.println("w");
    }
}
