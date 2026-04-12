package de.uzl.its.targets.finance_engine.service;

import org.springframework.stereotype.Service;
import de.uzl.its.targets.finance_engine.model.Rule;
import de.uzl.its.targets.finance_engine.model.Transaction;
import de.uzl.its.targets.finance_engine.util.SimpleExpressionEvaluatorV2;
import java.util.*;

@Service
public class RuleServiceV2 {
    private Map<String, Rule> rules = new HashMap<String, Rule>();

    public RuleServiceV2() {
        rules.put("r1", new Rule("r1", "amountMinor > 1000 && contains(category,'coffee')"));
        rules.put("r2", new Rule("r2", "merchant == 'Amazon' && amountMinor > 5000"));
        rules.put("r3", new Rule("r3", "RULE('r2') && SUM(category == 'coffee') > 1000"));
    }

    public List<Map<String,Object>> evaluateRules(String accountId, List<Rule> inputRules, List<Transaction> transactions, Map<String,String> parameters, boolean explain, boolean includeMatches) {
        List<Map<String,Object>> out = new ArrayList<Map<String,Object>>();
        List<Rule> rs = collectRules(inputRules);
        for (int i = 0; i < rs.size(); i++) {
            Rule r = rs.get(i);
            Map<String,Object> result = new HashMap<String,Object>();
            result.put("ruleId", r.id);
            result.put("expr", r.expr);
            List<Transaction> matches = new ArrayList<Transaction>();
            for (int j = 0; j < transactions.size(); j++) {
                Transaction t = transactions.get(j);
                boolean matched = SimpleExpressionEvaluatorV2.evaluate(r.expr, t, transactions, parameters, new Resolver());
                if (matched) matches.add(t);
            }
            result.put("matchesCount", matches.size());
            if (includeMatches) result.put("matches", matches);
            if (explain) result.put("explain", buildExplain(r.id, 0, new HashSet<String>()));
            out.add(result);
        }
        return out;
    }

    private List<Rule> collectRules(List<Rule> inputRules) {
        List<Rule> rs = new ArrayList<Rule>();
        if (inputRules == null || inputRules.size() == 0) {
            for (Rule r : rules.values()) rs.add(r);
        } else {
            for (int i = 0; i < inputRules.size(); i++) rs.add(inputRules.get(i));
        }
        return rs;
    }

    public String buildExplain(String root, int depth, Set<String> seen) {
        if (depth > 8) return "depth limit";
        if (seen.contains(root)) return "cycle:" + root;
        seen.add(root);
        Rule r = rules.get(root);
        if (r == null) return "unknown:" + root;
        StringBuilder sb = new StringBuilder();
        sb.append(root).append(" -> ").append(r.expr);
        // naive dependency scan: find RULE('x') occurrences
        int idx = 0;
        while ((idx = r.expr.indexOf("RULE('", idx)) >= 0) {
            int start = idx + 6;
            int end = r.expr.indexOf("')", start);
            if (end > start) {
                String dep = r.expr.substring(start, end);
                sb.append(" | depends: ").append(dep).append(" {").append(buildExplain(dep, depth+1, seen)).append("}");
                idx = end + 2;
            } else break;
        }
        return sb.toString();
    }

    private class Resolver implements SimpleExpressionEvaluatorV2.RuleResolver {
        public boolean resolve(String ruleId, Transaction current, List<Transaction> allTx, Map<String,String> params, Set<String> visiting, int depth) {
            if (depth > 10) return false;
            if (visiting.contains(ruleId)) return false;
            visiting.add(ruleId);
            Rule r = rules.get(ruleId);
            if (r == null) return false;
            boolean val = SimpleExpressionEvaluatorV2.evaluate(r.expr, current, allTx, params, this);
            visiting.remove(ruleId);
            return val;
        }
    }

    public Rule get(String id) { return rules.get(id); }
}
