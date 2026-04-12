package de.uzl.its.targets.finance_engine.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyService {
    private Map<String, Double> rateToUsd = new HashMap<String, Double>();
    public CurrencyService() {
        rateToUsd.put("USD", 1.0);
        rateToUsd.put("EUR", 1.1);
        rateToUsd.put("GBP", 1.3);
        rateToUsd.put("JPY", 0.007);
    }

    public long convertMinor(long amountMinor, String from, String to) {
        if (from == null) from = "USD";
        if (to == null) to = "USD";
        Double rf = rateToUsd.get(from.toUpperCase());
        Double rt = rateToUsd.get(to.toUpperCase());
        if (rf == null || rt == null) return amountMinor;
        double usd = amountMinor / 100.0 * rf;
        double target = usd / rt;
        long out = Math.round(target * 100.0);
        return out;
    }
}
