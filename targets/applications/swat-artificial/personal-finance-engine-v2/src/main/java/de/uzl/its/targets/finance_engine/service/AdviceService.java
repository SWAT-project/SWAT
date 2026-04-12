package de.uzl.its.targets.finance_engine.service;

import de.uzl.its.targets.finance_engine.model.AdvicePlan;
import de.uzl.its.targets.finance_engine.model.Transaction;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**

 Provides advice: simple projection and plan checks.
 */
@Service
public class AdviceService {
    private TransactionService txService;
    private CurrencyService currencyService;
    public AdviceService(TransactionService txService, CurrencyService currencyService) {
        this.txService = txService; this.currencyService = currencyService;
    }

    public Map<String,Object> plan(String accountId, AdvicePlan plan, List<Transaction> transactions, int simulateScenarios) {
        Map<String,Object> out = new HashMap<String,Object>();
        List<Map<String,Object>> goalResults = new ArrayList<Map<String,Object>>();
        for (int i = 0; i < plan.goals.size(); i++) {
            AdvicePlan.Goal g = plan.goals.get(i);
            Map<String,Object> gr = new HashMap<String,Object>();
            gr.put("goalType", g.type);
            gr.put("targetAmountMinor", g.amountMinor);
            gr.put("targetDate", g.dateIso);
            // simple projection: average monthly savings from transactions
            long monthlySavings = estimateMonthlySavings(transactions);
            gr.put("estimatedMonthlySavings", monthlySavings);
            // months between now and target
            long months = 0;
            try {
                months = ChronoUnit.MONTHS.between(LocalDate.now(), LocalDate.parse(g.dateIso));
                if (months < 0) months = 0;
            } catch (Exception ex) {
                months = 0;
            }
            long projected = monthlySavings * months;
            gr.put("projectedTotal", projected);
            gr.put("canMeetGoal", projected >= g.amountMinor);
            goalResults.add(gr);
        }
        out.put("goals", goalResults);
        out.put("simulateScenarios", simulateScenarios);
        // Monte-carlo-like randomization
        List<Map<String,Object>> scenarios = new ArrayList<Map<String,Object>>();
        Random rnd = new Random(1234);
        int nSc = Math.max(1, simulateScenarios);
        for (int s = 0; s < nSc; ++s) {
            double factor = 0.8 + rnd.nextDouble() * 0.8;
            Map<String,Object> sc = new HashMap<String,Object>();
            List<Map<String,Object>> grs = new ArrayList<Map<String,Object>>();
            for (int i = 0; i < goalResults.size(); i++) {
                Map<String,Object> g = goalResults.get(i);
                Map<String,Object> copy = new HashMap<String,Object>(g);
                long proj = ((Number) g.get("projectedTotal")).longValue();
                copy.put("scenarioProjected", Math.round(proj * factor));
                grs.add(copy);
            }
            sc.put("scenario", s);
            sc.put("goals", grs);
            scenarios.add(sc);
        }
        out.put("scenarios", scenarios);
        return out;
    }

    private long estimateMonthlySavings(List<Transaction> transactions) {
        // naive: compute average negative amounts (spend) per month and invert a bit
        long sum = 0;
        if (transactions == null || transactions.size() == 0) return 0;
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            sum += t.amountMinor;
        }
        // simple average monthly spend = total / months span
        LocalDate min = null; LocalDate max = null;
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            if (min == null || t.date.isBefore(min)) min = t.date;
            if (max == null || t.date.isAfter(max)) max = t.date;
        }
        long months = 1;
        if (min != null && max != null) {
            months = Math.max(1, ChronoUnit.MONTHS.between(min, max));
        }
        long avgPerMonth = sum / months;
        // treat savings as negative of spend/10
        return Math.abs(avgPerMonth) / 10;
    }

    public String renderNotificationTemplate(String template, Map<String,String> placeholders) {
        // naive placeholder replacement with pluralization support "(#items)"
        String out = template;
        for (Map.Entry<String,String> e : placeholders.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return out;
    }
}
