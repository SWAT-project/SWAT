package de.uzl.its.targets.finance.service;

import de.uzl.its.targets.finance.model.Transaction;
import de.uzl.its.targets.finance.model.SelectionResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
public class SelectionService {

    public SelectionResult selectTransactions(
            List<Transaction> input,
            long totalBudgetCents,
            Long minTxCents,
            Long maxTxCents,
            List<String> categoryPriorityPairs,
            List<String> txPriorityPairs
    ) {

        if (totalBudgetCents < 0 || minTxCents < 0 || maxTxCents < 0) {
            return null;
        }

        Map<String, Integer> catP = parsePairs(categoryPriorityPairs);
        Map<String, Integer> txP = parsePairs(txPriorityPairs);
        List<Transaction> pool = filterByBounds(input, minTxCents, maxTxCents);

        long remaining = totalBudgetCents;
        SelectionResult result = new SelectionResult();
        result.setTotalSelectedCents(0L);
        result.setSelectedCount(0);

        boolean[] taken = new boolean[pool.size()];

        int iter;
        for (iter = 0; iter < pool.size(); iter++) {
            int bestIdx = -1;
            double bestRatio = -1.0;

            int i;
            for (i = 0; i < pool.size(); i++) {
                if (taken[i]) continue;
                Transaction t = pool.get(i);
                long amtAbs = Math.abs(t.getAmountCents() != null ? t.getAmountCents() : 0L);
                if (amtAbs <= 0) continue;
                if (amtAbs > remaining) continue;

                int score = scoreOf(t, catP, txP);
                if (score <= 0) score = 1;

                double ratio = ((double) score) / ((double) amtAbs);
                if (ratio > bestRatio) {
                    bestRatio = ratio;
                    bestIdx = i;
                }
            }

            if (bestIdx == -1) {
                break;
            }

            Transaction pick = pool.get(bestIdx);
            long spend = Math.abs(pick.getAmountCents());
            if (spend <= remaining) {
                taken[bestIdx] = true;
                remaining -= spend;
                addPick(result, pick, spend);
            } else {
                break;
            }
        }

        result.getNotes().add("Remaining budget (cents): " + remaining);
        result.getNotes().add("Pool size considered: " + pool.size());
        result.getNotes().add("Heuristic: greedy by (priority / amount)");

        return result;
    }

    // ===== Proposal 1: Exact 0/1 knapsack DP (value = score, weight = spend abs cents) =====
    public SelectionResult selectTransactionsDP(
            List<Transaction> input,
            long totalBudgetCents,
            Long minTxCents,
            Long maxTxCents,
            List<String> categoryPriorityPairs,
            List<String> txPriorityPairs
    ) {

        if (totalBudgetCents < 0 || minTxCents < 0 || maxTxCents < 0) {
            return null;
        }

        Map<String, Integer> catP = parsePairs(categoryPriorityPairs);
        Map<String, Integer> txP = parsePairs(txPriorityPairs);
        List<Transaction> pool = filterByBounds(input, minTxCents, maxTxCents);

        // Build arrays
        int n = pool.size();
        long[] w = new long[n];
        int[] v = new int[n];
        int i;
        for (i = 0; i < n; i++) {
            Transaction t = pool.get(i);
            long amt = Math.abs(t.getAmountCents() != null ? t.getAmountCents() : 0L);
            int val = scoreOf(t, catP, txP);
            if (val <= 0) val = 1;
            w[i] = amt;
            v[i] = val;
        }

        // DP with budget in cents may be large; compress by bucketizing to avoid O(n*budget) overflow.
        // We'll scale weights to centBuckets (e.g., 100 cents bucket) if budget > 200000 elements.
        long budget = totalBudgetCents;
        long bucket = 1L;
        if (budget > 200000L) {
            bucket = 100L;
        }
        int W = (int) (budget / bucket);
        int[][] dp = new int[n + 1][W + 1];

        for (i = 1; i <= n; i++) {
            int j;
            for (j = 0; j <= W; j++) {
                dp[i][j] = dp[i - 1][j];
                int ww = (int) (w[i - 1] / bucket);
                if (ww <= j) {
                    int cand = dp[i - 1][j - ww] + v[i - 1];
                    if (cand > dp[i][j]) dp[i][j] = cand;
                }
            }
        }

        // Backtrack
        boolean[] take = new boolean[n];
        int j = W;
        for (i = n; i >= 1; i--) {
            if (dp[i][j] != dp[i - 1][j]) {
                take[i - 1] = true;
                int ww = (int) (w[i - 1] / bucket);
                j = j - ww;
            }
        }

        SelectionResult res = new SelectionResult();
        res.setTotalSelectedCents(0L);
        res.setSelectedCount(0);
        for (i = 0; i < n; i++) {
            if (take[i]) {
                Transaction t = pool.get(i);
                long spend = w[i];
                addPick(res, t, spend);
            }
        }
        res.getNotes().add("DP knapsack with bucket=" + bucket + " cent(s).");
        res.getNotes().add("DP table size: " + (n + 1) + " x " + (W + 1));
        return res;
    }

    // ===== Proposal 2: Category caps (max cents per category), greedy heuristic =====
    public SelectionResult selectTransactionsWithCategoryCaps(
            List<Transaction> input,
            long totalBudgetCents,
            Long minTxCents,
            Long maxTxCents,
            List<String> categoryPriorityPairs,
            List<String> txPriorityPairs,
            List<String> categoryCapsPairs // e.g., Food:50000, Leisure:20000
    ) {
        if (totalBudgetCents < 0 || minTxCents < 0 || maxTxCents < 0) {
            return null;
        }

        Map<String, Integer> catP = parsePairs(categoryPriorityPairs);
        Map<String, Integer> txP = parsePairs(txPriorityPairs);
        Map<String, Long> catCaps = parseLongPairs(categoryCapsPairs);

        List<Transaction> pool = filterByBounds(input, minTxCents, maxTxCents);

        long remaining = totalBudgetCents;
        SelectionResult result = new SelectionResult();
        result.setTotalSelectedCents(0L);
        result.setSelectedCount(0);
        Map<String, Long> catSpent = new HashMap<String, Long>();

        boolean[] taken = new boolean[pool.size()];

        int iter;
        for (iter = 0; iter < pool.size(); iter++) {
            int bestIdx = -1;
            double bestRatio = -1.0;

            int i;
            for (i = 0; i < pool.size(); i++) {
                if (taken[i]) continue;
                Transaction t = pool.get(i);
                long amtAbs = Math.abs(t.getAmountCents() != null ? t.getAmountCents() : 0L);
                if (amtAbs <= 0) continue;
                if (amtAbs > remaining) continue;

                // Check category cap
                String cat = t.getCategory();
                long spent = catSpent.containsKey(cat) ? catSpent.get(cat).longValue() : 0L;
                long cap = catCaps.containsKey(cat) ? catCaps.get(cat).longValue() : Long.MAX_VALUE;
                if (spent + amtAbs > cap) continue;

                int score = scoreOf(t, catP, txP);
                if (score <= 0) score = 1;
                double ratio = ((double) score) / ((double) amtAbs);
                if (ratio > bestRatio) {
                    bestRatio = ratio;
                    bestIdx = i;
                }
            }

            if (bestIdx == -1) {
                break;
            }

            Transaction pick = pool.get(bestIdx);
            long spend = Math.abs(pick.getAmountCents());
            String cat = pick.getCategory();
            long spent = catSpent.containsKey(cat) ? catSpent.get(cat).longValue() : 0L;
            long cap = catCaps.containsKey(cat) ? catCaps.get(cat).longValue() : Long.MAX_VALUE;
            if (spend <= remaining && spent + spend <= cap) {
                taken[bestIdx] = true;
                remaining -= spend;
                catSpent.put(cat, spent + spend);
                addPick(result, pick, spend);
            } else {
                break;
            }
        }

        result.getNotes().add("Remaining budget (cents): " + remaining);
        result.getNotes().add("Greedy with per-category caps.");
        return result;
    }

    // ===== Proposal 3: Combined — DP knapsack + enforce category caps (post-filter) =====
    public SelectionResult selectTransactionsDPWithCategoryCaps(
            List<Transaction> input,
            long totalBudgetCents,
            Long minTxCents,
            Long maxTxCents,
            List<String> categoryPriorityPairs,
            List<String> txPriorityPairs,
            List<String> categoryCapsPairs
    ) {
        if (totalBudgetCents < 0 || minTxCents < 0 || maxTxCents < 0) {
            return null;
        }

        SelectionResult base = selectTransactionsDP(input, totalBudgetCents, minTxCents, maxTxCents, categoryPriorityPairs, txPriorityPairs);
        Map<String, Long> caps = parseLongPairs(categoryCapsPairs);

        // We need to adjust selected set to obey category caps: if a category exceeds cap,
        // drop the lowest-priority-per-amount items first (manual scan, no sort/comparator).
        // Since SelectionResult only stores ids, we need the original items again:
        // Reconstruct selected items by matching ids where available (pseudo ids may appear).

        List<Transaction> selected = new ArrayList<Transaction>();
        int i;
        for (i = 0; i < input.size(); i++) {
            Transaction t = input.get(i);
            String id = t.getId();
            String pseudo = (t.getMerchant() != null ? t.getMerchant() : "") + "@" + t.getDateMs();
            int j;
            boolean match = false;
            for (j = 0; j < base.getSelectedTransactionIds().size(); j++) {
                String sel = base.getSelectedTransactionIds().get(j);
                if (id != null && id.equals(sel)) {
                    match = true;
                    break;
                }
                if (id == null && pseudo.equals(sel)) {
                    match = true;
                    break;
                }
            }
            if (match) selected.add(t);
        }

        // Compute per-category sums and drop until within caps.
        Map<String, Long> spent = new HashMap<String, Long>();
        for (i = 0; i < selected.size(); i++) {
            Transaction t = selected.get(i);
            String cat = t.getCategory();
            long amt = Math.abs(t.getAmountCents() != null ? t.getAmountCents() : 0L);
            long s = spent.containsKey(cat) ? spent.get(cat).longValue() : 0L;
            spent.put(cat, s + amt);
        }

        // Iterate categories; if over cap, drop items with smallest score/amount first
        int k;
        for (Map.Entry<String, Long> e : spent.entrySet()) {
            String cat = e.getKey();
            long cap = caps.containsKey(cat) ? caps.get(cat).longValue() : Long.MAX_VALUE;
            long s = e.getValue();
            while (s > cap) {
                // find worst item in this category
                int worstIdx = -1;
                double worstRatio = 1e18;
                for (k = 0; k < selected.size(); k++) {
                    Transaction t = selected.get(k);
                    if (cat == null) {
                        if (t.getCategory() != null) continue;
                    } else {
                        if (t.getCategory() == null || !cat.equals(t.getCategory())) continue;
                    }
                    long amt = Math.abs(t.getAmountCents() != null ? t.getAmountCents() : 0L);
                    if (amt <= 0) continue;
                    int score = scoreOf(t, parsePairs(categoryPriorityPairs), parsePairs(txPriorityPairs));
                    if (score <= 0) score = 1;
                    double ratio = ((double) score) / ((double) amt);
                    if (ratio < worstRatio) {
                        worstRatio = ratio;
                        worstIdx = k;
                    }
                }
                if (worstIdx == -1) break; // nothing to drop
                Transaction drop = selected.remove(worstIdx);
                long amt = Math.abs(drop.getAmountCents() != null ? drop.getAmountCents() : 0L);
                s -= amt;
                spent.put(cat, s);
            }
        }

        // Build final result
        SelectionResult out = new SelectionResult();
        out.setTotalSelectedCents(0L);
        out.setSelectedCount(0);
        for (i = 0; i < selected.size(); i++) {
            Transaction t = selected.get(i);
            long spend = Math.abs(t.getAmountCents() != null ? t.getAmountCents() : 0L);
            addPick(out, t, spend);
        }
        out.getNotes().add("DP selection post-adjusted with category caps.");
        return out;
    }

    // ===== Helpers =====
    private List<Transaction> filterByBounds(List<Transaction> input, Long minTxCents, Long maxTxCents) {
        List<Transaction> pool = new ArrayList<Transaction>();
        if (input == null) return pool;
        int i;
        for (i = 0; i < input.size(); i++) {
            Transaction t = input.get(i);
            Long a = t.getAmountCents();
            if (a == null) continue;
            long amt = a.longValue();
            long spend = amt < 0 ? -amt : amt;
            if (minTxCents != null && spend < (long) minTxCents) continue;
            if (maxTxCents != null && spend > (long) maxTxCents) continue;
            pool.add(t);
        }
        return pool;
    }

    private void addPick(SelectionResult res, Transaction pick, long spend) {
        res.setTotalSelectedCents(res.getTotalSelectedCents() + spend);
        res.setSelectedCount(res.getSelectedCount() + 1);
        if (pick.getId() != null) {
            res.getSelectedTransactionIds().add(pick.getId());
        } else {
            String pseudo = (pick.getMerchant() != null ? pick.getMerchant() : "") + "@" + pick.getDateMs();
            res.getSelectedTransactionIds().add(pseudo);
        }
    }

    private int scoreOf(Transaction t, Map<String, Integer> catP, Map<String, Integer> txP) {
        int score = 0;
        String id = t.getId();
        if (id != null && txP.containsKey(id)) score += txP.get(id).intValue();
        String cat = t.getCategory();
        if (cat != null && catP.containsKey(cat)) score += catP.get(cat).intValue();
        if (score <= 0) score = 1;
        return score;
    }

    private Map<String, Integer> parsePairs(List<String> pairs) {
        Map<String, Integer> m = new HashMap<String, Integer>();
        if (pairs == null) return m;
        int i;
        for (i = 0; i < pairs.size(); i++) {
            String s = pairs.get(i);
            if (s == null) continue;
            int sep = s.lastIndexOf(':');
            if (sep <= 0) continue;
            String k = s.substring(0, sep).trim();
            String v = s.substring(sep + 1).trim();
            try {
                int pr = Integer.parseInt(v);
                m.put(k, pr);
            } catch (Exception ignore) {
            }
        }
        return m;
    }

    private Map<String, Long> parseLongPairs(List<String> pairs) {
        Map<String, Long> m = new HashMap<String, Long>();
        if (pairs == null) return m;
        int i;
        for (i = 0; i < pairs.size(); i++) {
            String s = pairs.get(i);
            if (s == null) continue;
            int sep = s.lastIndexOf(':');
            if (sep <= 0) continue;
            String k = s.substring(0, sep).trim();
            String v = s.substring(sep + 1).trim();
            try {
                long pr = Long.parseLong(v);
                m.put(k, pr);
            } catch (Exception ignore) {
            }
        }
        return m;
    }
}
