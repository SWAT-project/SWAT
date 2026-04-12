package de.uzl.its.targets.finance.service;

import de.uzl.its.targets.finance.model.Account;
import de.uzl.its.targets.finance.model.Transaction;
import de.uzl.its.targets.finance.repository.AccountRepository;
import de.uzl.its.targets.finance.repository.TransactionRepository;
import de.uzl.its.targets.finance.util.InsertionSort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountsService {
    private final AccountRepository accounts;
    private final TransactionRepository txRepo;

    public AccountsService(AccountRepository accounts, TransactionRepository txRepo) {
        this.accounts = accounts;
        this.txRepo = txRepo;
    }

    public Account create(String userId, String accountType, String currency, String metadata, List<String> linkedInstitutionNames) {
        Account a = new Account();
        a.setId(UUID.randomUUID().toString());
        a.setUserId(userId);
        a.setAccountType(accountType);
        a.setCurrency(currency);
        if (linkedInstitutionNames != null) a.setLinkedInstitutionNames(linkedInstitutionNames);
        a.setMetadata(metadata);
        accounts.save(a);
        return a;
    }

    public java.util.List<Transaction> listTransactions(String accountId, Long fromMs, Long toMs, String category, String q, Long minAmountCents, Long maxAmountCents) {
        java.util.List<Transaction> out = txRepo.filtered(accountId, fromMs, toMs, category, q, minAmountCents, maxAmountCents);
        // already sorted by SQL, but keep deterministic ordering:
        InsertionSort.sortTransactionsByDate(out);
        return out;
    }
}
