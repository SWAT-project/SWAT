package de.uzl.its.targets.finance.controller;

import de.uzl.its.targets.finance.model.EvaluationResult;
import de.uzl.its.targets.finance.model.Transaction;
import de.uzl.its.targets.finance.service.RulesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rules")
public class RulesController {
    private final RulesService rulesService;

    public RulesController(RulesService rulesService) {
        this.rulesService = rulesService;
    }

    @PostMapping("/evaluate")
    @ResponseBody
    public ResponseEntity<EvaluationResult> evaluate(@RequestParam String accountId,
                                                     @RequestParam String rulesetId,
                                                     @RequestParam(required = false) Long timeframeStartMs,
                                                     @RequestParam(required = false) Long timeframeEndMs,
                                                     @RequestParam(defaultValue = "false") boolean explain,
                                                     @RequestParam(defaultValue = "false") boolean includeMatches) {

        return ResponseEntity.ok(rulesService.evaluate(accountId, rulesetId, timeframeStartMs, timeframeEndMs, explain, includeMatches));
    }

    @PostMapping("/simulate")
    @ResponseBody
    public ResponseEntity<List<String>> simulate(@RequestParam String rulesetId,
                                                 @RequestParam(defaultValue = "7") int horizonDays,
                                                 @RequestBody List<Transaction> hypotheticalTransactions) {

        return ResponseEntity.ok(rulesService.simulate(rulesetId, hypotheticalTransactions, horizonDays));
    }
}
