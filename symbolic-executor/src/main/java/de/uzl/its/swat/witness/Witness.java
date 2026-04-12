package de.uzl.its.swat.witness;

import java.math.BigDecimal;
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
        int encVal = Integer.valueOf(value);
        String witness = encVal + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
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
        // Use BigDecimal to avoid scientific notation (e.g., -2.022784E-6)
        // but handle special values (NaN, Infinity) which BigDecimal doesn't support
        String valueStr;
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            valueStr = Float.toString(value);
        } else {
            valueStr = BigDecimal.valueOf(value).toPlainString();
        }
        String witness = valueStr + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(long value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(double value, int lineNumber, String cname, String desc) {
        // Use BigDecimal to avoid scientific notation (e.g., 1.23E-10)
        // but handle special values (NaN, Infinity) which BigDecimal doesn't support
        String valueStr;
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            valueStr = Double.toString(value);
        } else {
            valueStr = BigDecimal.valueOf(value).toPlainString();
        }
        String witness = valueStr + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }

    public static void recordValue(String value, int lineNumber, String cname, String desc) {
        String witness = value + "@@@" + lineNumber + "@@@" + cname + "@@@" + desc;
        String enc = Base64.getEncoder().encodeToString(witness.getBytes());
        System.out.println("[WITNESS] " + enc);
    }
}
