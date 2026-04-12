package de.uzl.its.targets.finance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class AdvicePlan {
    private String accountId;
    private GoalBundle goals;
    private String riskProfile;
    private boolean rebalancing;
    private int scenarios;
    private Long monthlyRecommendationCents;
    private List<String> notes = new ArrayList<String>();
}
