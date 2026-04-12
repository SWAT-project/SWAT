package de.uzl.its.targets.finance.service;

import de.uzl.its.targets.finance.model.*;
import de.uzl.its.targets.finance.repository.*;
import org.springframework.stereotype.*;

import java.util.*;

@Service
public class RulesService {
    private final TransactionRepository txRepo;
    private final RuleRepository ruleRepo;

    public RulesService(TransactionRepository t, RuleRepository r) {
        this.txRepo = t;
        this.ruleRepo = r;
    }

    public EvaluationResult evaluate(String accountId, String rulesetId, Long fromMs, Long toMs, boolean explain, boolean includeMatches) {
        RuleSet set = ruleRepo.find(rulesetId);
        EvaluationResult res = new EvaluationResult();

        if (set != null) {
            res.setAccountId(accountId);
            res.setRulesetId(rulesetId);
            res.setExplain(explain);
            res.setIncludeMatches(includeMatches);
            List<Transaction> txs = txRepo.list(accountId);
            for (Transaction t : txs) {
                if (fromMs != null && t.getDateMs() < fromMs) continue;
                if (toMs != null && t.getDateMs() > toMs) continue;
                if (set != null) {
                    for (int r = 0; r < set.getRules().size(); r++) {
                        Rule rule = set.getRules().get(r);
                        if (matches(rule.getExpr(), t)) {
                            if (includeMatches) res.getMatchedRuleIds().add(rule.getId());
                            if (explain) res.getExplanations().add("Rule " + rule.getId() + " matched tx " + t.getId());
                        }
                    }
                }
            }
        }
        return res;
    }

    public List<String> simulate(String rulesetId, List<Transaction> hypotheticalTransactions, int horizonDays) {
        RuleSet set = ruleRepo.find(rulesetId);
        List<String> out = new ArrayList<>();

        if (set != null) {
            for (Transaction t : hypotheticalTransactions) {
                for (int r = 0; r < set.getRules().size(); r++) {
                    Rule rule = set.getRules().get(r);
                    if (matches(rule.getExpr(), t)) {
                        out.add("tx:" + t.getId() + " -> rule:" + rule.getId());
                    }
                }
            }
        }
        return out;
    }

    private boolean matches(String expr, Transaction t) {
        if (expr == null || expr.length() == 0) return false;
        String[] tokens = expr.split(" ");
        boolean acc = evalAtom(tokens[0], tokens.length > 1 ? tokens[1] : null, joinRest(tokens, 2), t);
        for (int i = 3; i < tokens.length; ) {
            String op = tokens[i - 1];
            String field = tokens[i];
            String cmp = tokens[i + 1];
            String rhs = joinRest(tokens, i + 2);
            boolean atom = evalAtom(field, cmp, rhs, t);
            if ("&&".equals(op)) acc = acc && atom;
            else if ("||".equals(op)) acc = acc || atom;
            else return false;
            i = i + 3;
        }
        return acc;
    }

    private boolean evalAtom(String field, String cmp, String rhsRaw, Transaction t) {
        if (field == null || cmp == null || rhsRaw == null) return false;
        String rhs = rhsRaw;
        if (rhs.startsWith("'")) {
            int last = rhs.lastIndexOf("'");
            if (last > 0) rhs = rhs.substring(1, last);
        }
        if ("category".equals(field)) {
            String val = t.getCategory();
            if ("==".equals(cmp)) return val != null && val.equals(rhs);
            if ("!=".equals(cmp)) return val == null || !val.equals(rhs);
        } else if ("merchant".equals(field)) {
            String val = t.getMerchant();
            if ("==".equals(cmp)) return val != null && val.equals(rhs);
            if ("~=".equals(cmp)) return de.uzl.its.targets.finance.util.SafeRegex.matches(val, rhs);
        } else if ("amount".equals(field)) {
            if (t.getAmountCents() == null) return false;
            try {
                double parsed = Double.parseDouble(rhs);
                long rhsCents = Math.round(parsed * 100.0);
                long a = t.getAmountCents();
                if (">".equals(cmp)) return a > rhsCents;
                if (">=".equals(cmp)) return a >= rhsCents;
                if ("<".equals(cmp)) return a < rhsCents;
                if ("<=".equals(cmp)) return a <= rhsCents;
                if ("==".equals(cmp)) return a == rhsCents;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private String joinRest(String[] tokens, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < tokens.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(tokens[i]);
        }
        return sb.toString();
    }
}
