package de.uzl.its.targets.finance.service;

import de.uzl.its.targets.finance.model.AdvicePlan;
import de.uzl.its.targets.finance.model.GoalBundle;
import org.springframework.stereotype.Service;

@Service
public class AdviceService {

    public AdvicePlan plan(String accountId, GoalBundle goalsBundle, String riskProfile, boolean rebalancing, int simulateScenarios) {
        AdvicePlan plan = new AdvicePlan();
        plan.setAccountId(accountId);
        plan.setGoals(goalsBundle);
        plan.setRiskProfile(riskProfile);
        plan.setRebalancing(rebalancing);
        plan.setScenarios(simulateScenarios);

        if (goalsBundle != null && goalsBundle.getAmountCents() != null && !goalsBundle.getAmountCents().isEmpty()
                && goalsBundle.getDateMs() != null && !goalsBundle.getDateMs().isEmpty()) {

            Long amount = goalsBundle.getAmountCents().get(0);
            Long targetDate = goalsBundle.getDateMs().get(0);
            if (amount == null) amount = 0L;
            if (targetDate == null) targetDate = System.currentTimeMillis();
            long now = System.currentTimeMillis();
            long diffMs = targetDate - now;
            if (diffMs < 0) diffMs = 0;
            long months = (long) Math.ceil(diffMs / (1000.0 * 60.0 * 60.0 * 24.0 * 30.0));
            if (months < 1) months = 1;
            long perMonth = Math.round((double) amount / (double) months);
            plan.setMonthlyRecommendationCents(perMonth);
            plan.getNotes().add("Calculated over " + months + " month(s).");
        } else {

            plan.setMonthlyRecommendationCents(0L);
        }

        return plan;
    }
}
