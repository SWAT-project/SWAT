package de.uzl.its.targets.finance_engine.controller;

import de.uzl.its.targets.finance_engine.model.Rule;
import de.uzl.its.targets.finance_engine.model.Transaction;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import de.uzl.its.targets.finance_engine.service.AccountService;
import de.uzl.its.targets.finance_engine.service.RuleService;
import java.util.*;

@RestController
@RequestMapping("/rules")
public class RulesController {
    private RuleService ruleService;
    private AccountService accountService;
    public RulesController(RuleService ruleService, AccountService accountService) {
        this.ruleService = ruleService;
        this.accountService = accountService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<List<Map<String,Object>>> evaluate(@RequestBody Map<String,Object> body,
                                                            @RequestParam(required = false, defaultValue = "false") boolean explain,
                                                            @RequestParam(required = false, defaultValue = "false") boolean includeMatches) {
        String accountId = (String) body.get("accountId");
        List<Map<String,String>> ruleSet = (List<Map<String,String>>) body.get("ruleSet");
        List<Rule> rules = new ArrayList<Rule>();
        if (ruleSet != null) {
            for (int i = 0; i < ruleSet.size(); i++) {
                Map<String,String> r = ruleSet.get(i);
                rules.add(new Rule(r.get("id"), r.get("expr")));
            }
        }
        Map<String,String> params = new HashMap<String,String>();
        Map<String,String> pmap = (Map<String,String>) body.get("parameters");
        if (pmap != null) {
            for (Map.Entry<String,String> e : pmap.entrySet()) params.put(e.getKey(), e.getValue());
        }
        List<Transaction> txs = accountService.getTransactions(accountId);
        List<Map<String,Object>> res = ruleService.evaluateRules(accountId, rules, txs, params, explain, includeMatches);
        return ResponseEntity.ok(res);
    }
}
