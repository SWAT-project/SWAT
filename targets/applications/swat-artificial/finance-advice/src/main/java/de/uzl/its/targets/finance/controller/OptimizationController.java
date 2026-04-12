package de.uzl.its.targets.finance.controller;

import de.uzl.its.targets.finance.model.Transaction;
import de.uzl.its.targets.finance.model.SelectionResult;
import de.uzl.its.targets.finance.repository.TransactionRepository;
import de.uzl.its.targets.finance.service.SelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/optimize")
public class OptimizationController {
    private final TransactionRepository txRepo;
    private final SelectionService selector;

    public OptimizationController(TransactionRepository txRepo, SelectionService selector) {
        this.txRepo = txRepo;
        this.selector = selector;
    }

    @PostMapping("/select/from-request")
    @ResponseBody
    public ResponseEntity<SelectionResult> selectFromRequest(
            @RequestParam long totalBudgetCents,
            @RequestParam(required = false) Long minTxCents,
            @RequestParam(required = false) Long maxTxCents,
            @RequestParam(required = false, name = "catPrio") List<String> categoryPriorities,
            @RequestParam(required = false, name = "txPrio") List<String> txPriorities,
            @RequestBody List<Transaction> transactions
    ) {
        try {
            SelectionResult res = selector.selectTransactions(transactions, totalBudgetCents, minTxCents, maxTxCents, categoryPriorities, txPriorities);
            if (res != null) {
                return ResponseEntity.ok(res);
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/select/from-db")
    @ResponseBody
    public ResponseEntity<SelectionResult> selectFromDb(
            @RequestParam String accountId,
            @RequestParam long totalBudgetCents,
            @RequestParam(required = false) Long minTxCents,
            @RequestParam(required = false) Long maxTxCents,
            @RequestParam(required = false, name = "catPrio") List<String> categoryPriorities,
            @RequestParam(required = false, name = "txPrio") List<String> txPriorities
    ) {
        try {
            java.util.List<Transaction> list = txRepo.list(accountId);
            if (list != null) {
                SelectionResult res = selector.selectTransactions(list, totalBudgetCents, minTxCents, maxTxCents, categoryPriorities, txPriorities);
                if (res != null) {
                    return ResponseEntity.ok(res);
                } else {
                    return ResponseEntity.badRequest().build();
                }
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== Proposal 1: DP (exact knapsack) =====
    @PostMapping("/select/dp/from-request")
    @ResponseBody
    public ResponseEntity<SelectionResult> selectDPFromRequest(
            @RequestParam long totalBudgetCents,
            @RequestParam(required = false) Long minTxCents,
            @RequestParam(required = false) Long maxTxCents,
            @RequestParam(required = false, name = "catPrio") List<String> categoryPriorities,
            @RequestParam(required = false, name = "txPrio") List<String> txPriorities,
            @RequestBody List<Transaction> transactions
    ) {
        try {
            SelectionResult res = selector.selectTransactionsDP(transactions, totalBudgetCents, minTxCents, maxTxCents, categoryPriorities, txPriorities);
            if (res != null) {
                // System.out.println(res);
                return ResponseEntity.ok(res);
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/select/dp/from-db")
    @ResponseBody
    public ResponseEntity<SelectionResult> selectDPFromDb(
            @RequestParam String accountId,
            @RequestParam long totalBudgetCents,
            @RequestParam(required = false) Long minTxCents,
            @RequestParam(required = false) Long maxTxCents,
            @RequestParam(required = false, name = "catPrio") List<String> categoryPriorities,
            @RequestParam(required = false, name = "txPrio") List<String> txPriorities
    ) {
        try {
            java.util.List<Transaction> list = txRepo.list(accountId);
            if (list != null) {
                SelectionResult res = selector.selectTransactionsDP(list, totalBudgetCents, minTxCents, maxTxCents, categoryPriorities, txPriorities);
                if (res != null) {
                    return ResponseEntity.ok(res);
                } else {
                    return ResponseEntity.badRequest().build();
                }
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== Proposal 2: Category caps =====
    @PostMapping("/select/catcaps/from-request")
    @ResponseBody
    public ResponseEntity<SelectionResult> selectCapsFromRequest(
            @RequestParam long totalBudgetCents,
            @RequestParam(required = false) Long minTxCents,
            @RequestParam(required = false) Long maxTxCents,
            @RequestParam(required = false, name = "catPrio") List<String> categoryPriorities,
            @RequestParam(required = false, name = "txPrio") List<String> txPriorities,
            @RequestParam(required = false, name = "catCap") List<String> categoryCaps,
            @RequestBody List<Transaction> transactions
    ) {
        try {
            SelectionResult res = selector.selectTransactionsWithCategoryCaps(transactions, totalBudgetCents, minTxCents, maxTxCents, categoryPriorities, txPriorities, categoryCaps);
            if (res != null) {
                return ResponseEntity.ok(res);
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/select/catcaps/from-db")
    @ResponseBody
    public ResponseEntity<SelectionResult> selectCapsFromDb(
            @RequestParam String accountId,
            @RequestParam long totalBudgetCents,
            @RequestParam(required = false) Long minTxCents,
            @RequestParam(required = false) Long maxTxCents,
            @RequestParam(required = false, name = "catPrio") List<String> categoryPriorities,
            @RequestParam(required = false, name = "txPrio") List<String> txPriorities,
            @RequestParam(required = false, name = "catCap") List<String> categoryCaps
    ) {
        try {
            java.util.List<Transaction> list = txRepo.list(accountId);
            if (list != null) {
                SelectionResult res = selector.selectTransactionsWithCategoryCaps(list, totalBudgetCents, minTxCents, maxTxCents, categoryPriorities, txPriorities, categoryCaps);
                if (res != null) {
                    return ResponseEntity.ok(res);
                } else {
                    return ResponseEntity.badRequest().build();
                }
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== Proposal 3: Combined (DP + caps) =====
    @PostMapping("/select/combined/from-request")
    @ResponseBody
    public ResponseEntity<SelectionResult> selectCombinedFromRequest(
            @RequestParam long totalBudgetCents,
            @RequestParam(required = false) Long minTxCents,
            @RequestParam(required = false) Long maxTxCents,
            @RequestParam(required = false, name = "catPrio") List<String> categoryPriorities,
            @RequestParam(required = false, name = "txPrio") List<String> txPriorities,
            @RequestParam(required = false, name = "catCap") List<String> categoryCaps,
            @RequestBody List<Transaction> transactions
    ) {
        try {
            SelectionResult res = selector.selectTransactionsDPWithCategoryCaps(transactions, totalBudgetCents, minTxCents, maxTxCents, categoryPriorities, txPriorities, categoryCaps);
            if (res != null) {
                return ResponseEntity.ok(res);
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/select/combined/from-db")
    @ResponseBody
    public ResponseEntity<SelectionResult> selectCombinedFromDb(
            @RequestParam String accountId,
            @RequestParam long totalBudgetCents,
            @RequestParam(required = false) Long minTxCents,
            @RequestParam(required = false) Long maxTxCents,
            @RequestParam(required = false, name = "catPrio") List<String> categoryPriorities,
            @RequestParam(required = false, name = "txPrio") List<String> txPriorities,
            @RequestParam(required = false, name = "catCap") List<String> categoryCaps
    ) {
        try {
            java.util.List<Transaction> list = txRepo.list(accountId);
            if (list != null) {
                SelectionResult res = selector.selectTransactionsDPWithCategoryCaps(list, totalBudgetCents, minTxCents, maxTxCents, categoryPriorities, txPriorities, categoryCaps);
                if (res != null) {
                    return ResponseEntity.ok(res);
                } else {
                    return ResponseEntity.badRequest().build();
                }
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
