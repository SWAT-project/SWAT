package de.uzl.its.targets.finance.util;

import java.util.regex.Pattern;

public final class SafeRegex {
    private SafeRegex() {}
    public static boolean matches(String text, String regex) {
        if (text == null || regex == null) return false;
        Pattern p = Pattern.compile(regex);
        return p.matcher(text).find();
    }
}
