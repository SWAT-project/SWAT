package de.uzl.its.targets.finance_engine.util;

public class Levenshtein {
    public static int distance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int n = a.length();
        int m = b.length();
        int[] prev = new int[m + 1];
        int[] cur = new int[m + 1];
        for (int j = 0; j <= m; ++j) prev[j] = j;
        for (int i = 1; i <= n; ++i) {
            cur[0] = i;
            for (int j = 1; j <= m; ++j) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                int ins = cur[j - 1] + 1;
                int del = prev[j] + 1;
                int sub = prev[j - 1] + cost;
                int best = ins;
                if (del < best) best = del;
                if (sub < best) best = sub;
                cur[j] = best;
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[m];
    }
}
