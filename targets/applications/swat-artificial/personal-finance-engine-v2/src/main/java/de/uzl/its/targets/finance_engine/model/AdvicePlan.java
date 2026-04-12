package de.uzl.its.targets.finance_engine.model;

import java.util.ArrayList;
import java.util.List;

public class AdvicePlan {
    public String accountId;
    public List<Goal> goals = new ArrayList<Goal>();
    public String riskProfile;
    public boolean rebalancing;
    public AdvicePlan() {}
    public static class Goal {
        public String type;
        public long amountMinor;
        public String dateIso;
    }
}
