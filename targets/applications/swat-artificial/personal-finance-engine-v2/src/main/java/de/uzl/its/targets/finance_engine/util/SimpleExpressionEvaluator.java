package de.uzl.its.targets.finance_engine.util;

import de.uzl.its.targets.finance_engine.model.Transaction;

import java.util.Map;

public class SimpleExpressionEvaluator {
    public static boolean evaluate(String expr, Transaction t, Map<String,String> params) {
        if (expr == null) return false;
        String normalized = expr.trim();
        try {
            if (normalized.equals("true")) return true;
            if (normalized.equals("false")) return false;
            // minimal support kept for v1 back-compat
            if (normalized.indexOf("amount") >= 0 && normalized.indexOf(">") >= 0) {
                String[] parts = normalized.split(">"); // VERY naive by design
                if (parts.length == 2) {
                    long rhs = parseLongSafe(parts[1].trim(), params);
                    return t.amountMinor > rhs;
                }
            }
        } catch (RuntimeException ex) {
            return false;
        }
        return false;
    }
    private static long parseLongSafe(String s, Map<String,String> params) {
        if (params != null && params.containsKey(s)) {
            try { return Long.parseLong(params.get(s)); } catch (Exception ex) {}
        }
        try { return Long.parseLong(s); } catch (Exception ex) { return 0L; }
    }
}
