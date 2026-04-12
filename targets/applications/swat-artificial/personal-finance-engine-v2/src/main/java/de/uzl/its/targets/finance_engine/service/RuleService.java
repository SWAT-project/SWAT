package de.uzl.its.targets.finance_engine.service;

import org.springframework.stereotype.Service;
import de.uzl.its.targets.finance_engine.model.Rule;
import de.uzl.its.targets.finance_engine.model.Transaction;
import de.uzl.its.targets.finance_engine.util.SimpleExpressionEvaluator;
import java.util.*;

@Service
public class RuleService {
    private Map<String, Rule> rules = new HashMap<String, Rule>();
    public RuleService() {
        rules.put("r1", new Rule("r1", "amount > 10000"));
        rules.put("r2", new Rule("r2", "amount > 5000"));
    }
    public List<Map<String,Object>> evaluateRules(String accountId, List<Rule> inputRules, List<Transaction> transactions, Map<String,String> parameters, boolean explain, boolean includeMatches) {
        List<Map<String,Object>> out = new ArrayList<Map<String,Object>>();
        List<Rule> rs = new ArrayList<Rule>();
        if (inputRules == null || inputRules.size() == 0) { for (Rule r : rules.values()) rs.add(r); }
        else { for (int i = 0; i < inputRules.size(); i++) rs.add(inputRules.get(i)); }
        for (int i = 0; i < rs.size(); i++) {
            Rule r = rs.get(i);
            Map<String,Object> result = new HashMap<String,Object>();
            result.put("ruleId", r.id);
            result.put("expr", r.expr);
            List<Transaction> matches = new ArrayList<Transaction>();
            for (int j = 0; j < transactions.size(); j++) {
                Transaction t = transactions.get(j);
                boolean matched = SimpleExpressionEvaluator.evaluate(r.expr, t, parameters);
                if (matched) matches.add(t);
            }
            result.put("matchesCount", matches.size());
            if (includeMatches) result.put("matches", matches);
            if (explain) result.put("explain", "V1 evaluator");
            out.add(result);
        }
        return out;
    }
    public Rule get(String id) { return rules.get(id); }
}
