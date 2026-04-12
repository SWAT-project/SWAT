package de.uzl.its.targets.finance_engine.controller;

import de.uzl.its.targets.finance_engine.model.Rule;
import de.uzl.its.targets.finance_engine.model.Transaction;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import de.uzl.its.targets.finance_engine.service.AccountService;
import de.uzl.its.targets.finance_engine.service.RuleServiceV2;
import java.util.*;

@RestController
@RequestMapping("/v2/rules")
public class RulesV2Controller {
    private RuleServiceV2 ruleService;
    private AccountService accountService;
    public RulesV2Controller(RuleServiceV2 ruleService, AccountService accountService) {
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

    @PostMapping("/simulate")
    public ResponseEntity<List<Map<String,Object>>> simulate(@RequestBody Map<String,Object> body) {
        List<Map<String,Object>> out = new ArrayList<Map<String,Object>>();
        List<Map<String,Object>> hypot = (List<Map<String,Object>>) body.get("hypotheticalTransactions");
        List<Transaction> txs = new ArrayList<Transaction>();
        if (hypot != null) {
            for (int i = 0; i < hypot.size(); ++i) {
                Map<String,Object> h = hypot.get(i);
                Transaction t = new Transaction();
                t.id = "hyp-" + i;
                t.raw = (String) h.get("raw");
                String d = (String) h.get("date");
                try { t.date = java.time.LocalDate.parse(d); } catch (Exception ex) { t.date = java.time.LocalDate.now(); }
                Object amt = h.get("amountMinor");
                if (amt instanceof Number) t.amountMinor = ((Number)amt).longValue();
                else t.amountMinor = 0;
                t.merchant = (String) h.get("merchant");
                t.category = (String) h.get("category");
                txs.add(t);
            }
        }
        List<Map<String,Object>> res = ruleService.evaluateRules(null, null, txs, new HashMap<String,String>(), false, true);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{rulesetId}/dependencies")
    public ResponseEntity<Map<String,Object>> deps(@PathVariable String rulesetId,
                                                   @RequestParam(required = false, defaultValue = "1") int expandDepth) {
        Map<String,Object> out = new HashMap<String,Object>();
        out.put("rulesetId", rulesetId);
        Map<String,Object> info = new HashMap<String,Object>();
        info.put("explain", ruleService.buildExplain(rulesetId, 0, new java.util.HashSet<String>()));
        out.put("info", info);
        return ResponseEntity.ok(out);
    }
}
