package de.uzl.its.targets.finance_engine.controller;

import de.uzl.its.targets.finance_engine.model.Account;
import de.uzl.its.targets.finance_engine.model.AdvicePlan;
import de.uzl.its.targets.finance_engine.model.Transaction;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import de.uzl.its.targets.finance_engine.service.AdviceService;
import de.uzl.its.targets.finance_engine.service.AccountService;
import java.util.*;

@RestController
@RequestMapping("/advice")
public class AdviceController {
    private AdviceService adviceService;
    private AccountService accountService;

    public AdviceController(AdviceService adviceService, AccountService accountService) {
        this.adviceService = adviceService;
        this.accountService = accountService;
    }

    @PostMapping("/plan")
    public ResponseEntity<Map<String,Object>> plan(@RequestBody AdvicePlan plan,
                                                   @RequestParam(required = false, defaultValue = "1") int simulateScenarios) {
        List<Transaction> txs = new ArrayList<Transaction>();
        Account a = accountService.get(plan.accountId);
        if (a != null) txs = a.transactions;
        Map<String,Object> res = adviceService.plan(plan.accountId, plan, txs, simulateScenarios);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/portfolio/{accountId}")
    public ResponseEntity<Map<String,Object>> portfolio(@PathVariable String accountId,
                                                        @RequestParam(required = false) String asOf,
                                                        @RequestParam(required = false, defaultValue = "false") boolean includeProjections) {
        Account a = accountService.get(accountId);
        Map<String,Object> out = new HashMap<String,Object>();
        out.put("accountId", accountId);
        out.put("asOf", asOf == null ? java.time.LocalDate.now().toString() : asOf);
        List<Transaction> txs = a == null ? new ArrayList<Transaction>() : a.transactions;
        long balance = 0;
        for (int i = 0; i < txs.size(); i++) balance += txs.get(i).amountMinor;
        out.put("balanceMinor", balance);
        if (includeProjections) {
            Map<String,Object> proj = new HashMap<String,Object>();
            proj.put("1m", balance);
            proj.put("6m", balance);
            out.put("projections", proj);
        }
        return ResponseEntity.ok(out);
    }

    @PostMapping("/notify")
    public ResponseEntity<String> notify(@RequestBody Map<String,Object> body) {
        String accountId = (String) body.get("accountId");
        String channel = (String) body.get("channel");
        List<String> rules = (List<String>) body.get("rules");

        Map<String,String> placeholders = new HashMap<String,String>();
        placeholders.put("accountId", accountId == null ? "" : accountId);
        placeholders.put("rules", rules == null ? "" : join(rules, ","));

        String template = "Account {{accountId}} triggered rules: {{rules}}";
        String message = adviceService.renderNotificationTemplate(template, placeholders);
        return ResponseEntity.ok("would send to " + channel + ": " + message);
    }

    private String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
