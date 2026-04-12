package de.uzl.its.targets.finance_engine.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import de.uzl.its.targets.finance_engine.service.AccountService;
import de.uzl.its.targets.finance_engine.service.TransactionService;
import de.uzl.its.targets.finance_engine.model.*;

import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/accounts")
public class AccountsController {
    private AccountService accountService;
    private TransactionService txService;

    public AccountsController(AccountService accountService, TransactionService txService) {
        this.accountService = accountService;
        this.txService = txService;
    }

    @PostMapping("/create")
    public ResponseEntity<Account> create(@RequestBody Account req,
                                          @RequestParam(required = false, defaultValue = "false") boolean verify) {
        Account created = accountService.createAccount(req, verify);
        return new ResponseEntity<Account>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(
            @PathVariable String id,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) Long minAmount,
            @RequestParam(required = false) Long maxAmount,
            @RequestParam(required = false) String queryText
    ) {
        List<Transaction> txs = accountService.getTransactions(id);
        Date fromDate = null;
        Date toDate = null;
        try { if (from != null) fromDate = java.sql.Date.valueOf(LocalDate.parse(from)); } catch (DateTimeParseException ex) {}
        try { if (to != null) toDate = java.sql.Date.valueOf(LocalDate.parse(to)); } catch (DateTimeParseException ex) {}
        List<Transaction> filtered = txService.filterTransactions(txs, fromDate, toDate, category, minAmount, maxAmount, queryText);
        return ResponseEntity.ok(filtered);
    }

    /**
     * CHANGED: Body is now ImportPayload (csvPayload + mappings[]).
     * Clients can omit mappings to use the default.
     */
    @PostMapping("/import")
    public ResponseEntity<String> importCsv(
            @RequestParam String accountId,
            @RequestParam(required = false, defaultValue = "append") String strategy,
            @RequestHeader(value = "X-Import-Strategy", required = false) String headerStrategy,
            @RequestBody ImportPayload payload
    ) {
        if (payload == null) payload = new ImportPayload();
        String csvPayload = payload.csvPayload;
        // Convert List<ColMapping> -> Map<Integer,String>
        Map<Integer,String> mapping = new HashMap<Integer,String>();
        if (payload.mappings != null && payload.mappings.size() > 0) {
            for (int i = 0; i < payload.mappings.size(); i++) {
                ColMapping cm = payload.mappings.get(i);
                // basic sanity
                if (cm != null && cm.field != null) {
                    mapping.put(cm.index, cm.field);
                }
            }
        } else {
            // default mapping if none provided
            mapping.put(0, "date");
            mapping.put(1, "merchant");
            mapping.put(2, "amount");
            mapping.put(3, "currency");
        }

        String effectiveStrategy = (headerStrategy != null && headerStrategy.length() > 0) ? headerStrategy : strategy;
        accountService.importCsv(accountId, csvPayload == null ? "" : csvPayload, mapping, effectiveStrategy);
        return ResponseEntity.ok("imported");
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<Transaction> addTransaction(@PathVariable String accountId, @RequestBody Transaction tx) {
        Transaction added = accountService.addTransaction(accountId, tx);
        if (added == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return new ResponseEntity<Transaction>(added, HttpStatus.CREATED);
    }
}
