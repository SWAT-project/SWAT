package de.uzl.its.swat.witness;

import java.util.Base64;

public class Witness {
    public static void recordValue(boolean value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(byte value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(char value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(short value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(int value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(float value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(long value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(double value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(String value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }
}
