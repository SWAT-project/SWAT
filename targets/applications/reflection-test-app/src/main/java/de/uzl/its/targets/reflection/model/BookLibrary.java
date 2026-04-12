package de.uzl.its.targets.reflection.model;

import java.util.Arrays;
import java.util.List;

public class BookLibrary {

    public List<String> getLibraryCount(Integer type) {
        if (type <= 1) {
            return Arrays.asList("Lord of the Rings", "The Hobbit");
        } else if (type == 2) {
            return Arrays.asList("War of Worlds", "John Carter of Mars");
        } else if (type >= 3) {
            return Arrays.asList("Macbeth", "Hamlet");
        } else {
            throw new RuntimeException("Invalid Book Type");
        }
    }
}
