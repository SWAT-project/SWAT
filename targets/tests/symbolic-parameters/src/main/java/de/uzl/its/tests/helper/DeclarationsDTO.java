package de.uzl.its.tests.helper;


public class DeclarationsDTO {
    public int i; // Standard public val initialized in constructor;
    public int j; // Standard public val not explicitly initialized
    public int k = -1; // Standard public val initialized inline
    public static int l; // Static public val;
    private int m; // Private val;

    public int getM() {
        return m;
    }
    public void setM(int m) {
        this.m = m;
    }

    public DeclarationsDTO() {
        i = -1;
        m = -1;

    }
}
