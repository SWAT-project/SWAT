package de.uzl.its.targets.finance_engine.service;

import org.springframework.stereotype.Service;
import de.uzl.its.targets.finance_engine.util.Levenshtein;

@Service
public class FuzzyService {
    private String[] canonical = new String[] {
        "Starbucks", "Amazon", "Ikea", "Walmart", "Shell", "Uber", "Netflix"
    };

    public String resolveMerchant(String raw) {
        if (raw == null) return null;
        String best = raw;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < canonical.length; ++i) {
            String cand = canonical[i];
            int d = Levenshtein.distance(raw.toLowerCase(), cand.toLowerCase());
            if (d < bestDist) { bestDist = d; best = cand; }
        }
        if (bestDist <= Math.max(1, raw.length() / 3)) return best;
        return raw;
    }

    public boolean fuzzyMatch(String candidate, String query) {
        if (candidate == null || query == null) return false;
        candidate = candidate.toLowerCase();
        query = query.toLowerCase();
        if (candidate.indexOf(query) >= 0) return true;
        int d = Levenshtein.distance(candidate, query);
        return d <= Math.max(1, query.length() / 3);
    }
}
