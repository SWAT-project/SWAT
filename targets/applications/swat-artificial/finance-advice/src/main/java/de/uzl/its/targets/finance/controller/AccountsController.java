package de.uzl.its.targets.finance.controller;

import de.uzl.its.targets.finance.model.Account;
import de.uzl.its.targets.finance.model.Transaction;
import de.uzl.its.targets.finance.service.AccountsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountsController {
    private final AccountsService accountsService;

    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Account> create(@RequestParam String userId,
                                          @RequestParam String accountType,
                                          @RequestParam String currency,
                                          @RequestParam(required = false) String metadata,
                                          @RequestBody(required = false) List<String> linkedInstitutionNames) {
        Account a = accountsService.create(userId, accountType, currency, metadata, linkedInstitutionNames);
        return ResponseEntity.ok(a);
    }

    @GetMapping("/{id}/transactions")
    @ResponseBody
    public ResponseEntity<List<Transaction>> listTx(@PathVariable("id") String accountId,
                                                    @RequestParam(required = false) Long fromMs,
                                                    @RequestParam(required = false) Long toMs,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(required = false) String queryText,
                                                    @RequestParam(required = false) Long minAmountCents,
                                                    @RequestParam(required = false) Long maxAmountCents) {
        List<Transaction> txs = accountsService.listTransactions(accountId, fromMs, toMs, category, queryText, minAmountCents, maxAmountCents);
        return ResponseEntity.ok(txs);
    }
}
