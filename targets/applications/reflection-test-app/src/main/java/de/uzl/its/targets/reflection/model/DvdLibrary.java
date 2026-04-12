package de.uzl.its.targets.reflection.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DvdLibrary {
    private int nbOfDiscs;

    private String name;

    private static int numberOfCustomers = 0;

    public DvdLibrary(int nbOfDiscs) {
        this.nbOfDiscs = nbOfDiscs;
        this.name = "myDvdLibrary";
    }

    public List<String> getLibraryCount(Integer type) {
        if (type <= 1 && nbOfDiscs == 1) {
            return Arrays.asList("Lord of the Rings", "The Hobbit");
        } else if (type == 2 && nbOfDiscs == 2) {
            return Arrays.asList("War of Worlds", "John Carter of Mars");
        } else if (type >= 3 && nbOfDiscs == 3) {
            return Arrays.asList("Macbeth", "Hamlet");
        } else {
            throw new RuntimeException("Invalid Book Type");
        }
    }

    public List<String> getLibraryCountConsideringName(Integer type) {
        if (type <= 1 && nbOfDiscs == 1 && name.equals("uzl-lib")) {
            return Arrays.asList("Lord of the Rings", "The Hobbit");
        } else if (type == 2 && nbOfDiscs == 4 && name.equals("tuhh-lib")) {
            return Arrays.asList("War of Worlds", "John Carter of Mars");
        } else if (type >= 3 && nbOfDiscs == 7 && name.equals("lib")) {
            return Arrays.asList("Macbeth", "Hamlet");
        }

        return new ArrayList<>();
    }

    public List<String> getLibraryCountConsideringName(String type) {
        if (type.equals("1") && nbOfDiscs == 1 && name.equals("uzl-lib")) {
            return Arrays.asList("Lord of the Rings", "The Hobbit");
        } else if (type.equals("2") && nbOfDiscs == 4 && name.equals("tuhh-lib")) {
            return Arrays.asList("War of Worlds", "John Carter of Mars");
        } else if (type.equals("3") && nbOfDiscs == 7 && name.equals("lib")) {
            return Arrays.asList("Macbeth", "Hamlet");
        }

        return new ArrayList<>();
    }

    public static String isNbOfCustomersLarger10() {
        if (numberOfCustomers <= 10) {
            return "No";
        } else {
            return "Yes";
        }
    }
}
