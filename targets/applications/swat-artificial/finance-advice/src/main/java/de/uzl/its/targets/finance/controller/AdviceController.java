package de.uzl.its.targets.finance.controller;

import de.uzl.its.targets.finance.model.AdvicePlan;
import de.uzl.its.targets.finance.model.GoalBundle;
import de.uzl.its.targets.finance.service.AdviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/advice")
public class AdviceController {
    private final AdviceService adviceService;

    public AdviceController(AdviceService adviceService) { this.adviceService = adviceService; }

    @PostMapping("/plan")
    @ResponseBody
    public ResponseEntity<AdvicePlan> plan(@RequestParam String accountId,
                                           @RequestParam String riskProfile,
                                           @RequestParam(defaultValue = "false") boolean rebalancing,
                                           @RequestParam(defaultValue = "3") int simulateScenarios,
                                           @RequestBody(required = false) GoalBundle goalsBundle) {
        return ResponseEntity.ok(adviceService.plan(accountId, goalsBundle, riskProfile, rebalancing, simulateScenarios));
    }
}
