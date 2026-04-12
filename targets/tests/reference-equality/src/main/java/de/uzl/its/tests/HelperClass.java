package de.uzl.its.tests;


public class HelperClass {
    private int id;

    public HelperClass(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        HelperClass that = (HelperClass) obj;
        return id == that.id;
    }
}

