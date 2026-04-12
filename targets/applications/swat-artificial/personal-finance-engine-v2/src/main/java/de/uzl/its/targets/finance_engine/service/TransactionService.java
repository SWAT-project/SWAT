package de.uzl.its.targets.finance_engine.service;

import de.uzl.its.targets.finance_engine.util.InsertionSorter;
import org.springframework.stereotype.Service;
import de.uzl.its.targets.finance_engine.model.Transaction;
import java.util.*;

@Service
public class TransactionService {
    private FuzzyService fuzzy;
    private CurrencyService currencyService;
    public TransactionService(FuzzyService fuzzy, CurrencyService currencyService) {
        this.fuzzy = fuzzy;
        this.currencyService = currencyService;
    }

    public Transaction normalize(Transaction t, String targetCurrency) {
        t.merchant = fuzzy.resolveMerchant(t.merchant);
        if (t.currency != null && targetCurrency != null && !t.currency.equalsIgnoreCase(targetCurrency)) {
            t.amountMinor = currencyService.convertMinor(t.amountMinor, t.currency, targetCurrency);
            t.currency = targetCurrency;
        }
        if (t.category == null || t.category.length() == 0) {
            t.category = inferCategoryFromMerchant(t.merchant);
        }
        return t;
    }

    private String inferCategoryFromMerchant(String merchant) {
        if (merchant == null) return "uncategorized";
        String m = merchant.toLowerCase();
        if (m.indexOf("starbuck") >= 0) return "coffee";
        if (m.indexOf("netflix") >= 0) return "entertainment";
        if (m.indexOf("shell") >= 0) return "fuel";
        if (m.indexOf("uber") >= 0) return "transport";
        return "misc";
    }

    public List<Transaction> filterTransactions(List<Transaction> list, Date from, Date to, List<String> categories, Long minAmount, Long maxAmount, String queryText) {
        List<Transaction> out = new ArrayList<Transaction>();
        for (int i = 0; i < list.size(); i++) {
            Transaction t = list.get(i);
            boolean ok = true;
            if (from != null) {
                Date d = java.sql.Date.valueOf(t.date);
                if (d.before(from)) ok = false;
            }
            if (to != null) {
                Date d = java.sql.Date.valueOf(t.date);
                if (d.after(to)) ok = false;
            }
            if (categories != null && categories.size() > 0) {
                boolean found = false;
                for (int k = 0; k < categories.size(); ++k) {
                    if (categories.get(k).equalsIgnoreCase(t.category)) { found = true; break; }
                }
                if (!found) ok = false;
            }
            if (minAmount != null && t.amountMinor < minAmount) ok = false;
            if (maxAmount != null && t.amountMinor > maxAmount) ok = false;
            if (queryText != null && queryText.length() > 0) {
                String q = queryText.toLowerCase();
                boolean inRaw = t.raw != null && t.raw.toLowerCase().indexOf(q) >= 0;
                boolean inMerchant = t.merchant != null && t.merchant.toLowerCase().indexOf(q) >= 0;
                if (!(inRaw || inMerchant)) ok = false;
            }
            if (ok) out.add(t);
        }
        InsertionSorter.sortByKey(out, new InsertionSorter.KeyExtractor<Transaction>() {
            public long key(Transaction item) { return item.date.toEpochDay(); }
        });
        return out;
    }
}
