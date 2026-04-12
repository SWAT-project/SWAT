package de.uzl.its.targets.finance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class EvaluationResult {
    private String accountId;
    private String rulesetId;
    private boolean explain;
    private boolean includeMatches;
    private java.util.List<String> matchedRuleIds = new ArrayList<String>();
    private java.util.List<String> explanations = new ArrayList<String>();
}
