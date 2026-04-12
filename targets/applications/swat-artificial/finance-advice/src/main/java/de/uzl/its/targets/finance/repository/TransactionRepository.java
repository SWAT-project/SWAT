package de.uzl.its.targets.finance.repository;

import de.uzl.its.targets.finance.dao.*;
import de.uzl.its.targets.finance.entity.*;
import de.uzl.its.targets.finance.model.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;
import java.util.UUID;

@Repository
public class TransactionRepository {
    private final TransactionDao txDao;

    public TransactionRepository(TransactionDao t) {
        this.txDao = t;
    }

    @Transactional
    public void add(String accountId, Transaction t) {
        String id = t.getId();
        if (id == null || id.length() == 0) id = UUID.randomUUID().toString();
        txDao.insertTx(id, accountId, t.getDateMs(), t.getAmountCents(), t.getCurrency(), t.getMerchant(), t.getCategory(), t.getRawText());
    }

    public List<Transaction> list(String accountId) {
        return map(txDao.listByAccount(accountId));
    }

    public List<Transaction> filtered(String accountId, Long fromMs, Long toMs, String category, String q, Long minAmountCents, Long maxAmountCents) {
        return map(txDao.filtered(accountId, fromMs, toMs, category, q, minAmountCents, maxAmountCents));
    }

    private List<Transaction> map(List<TransactionEntity> rows) {
        List<Transaction> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            TransactionEntity e = rows.get(i);
            Transaction t = new Transaction();
            t.setId(e.getId());
            t.setAccountId(e.getAccountId());
            t.setDateMs(e.getDateMs());
            t.setAmountCents(e.getAmountCents());
            t.setCurrency(e.getCurrency());
            t.setMerchant(e.getMerchant());
            t.setCategory(e.getCategory());
            t.setRawText(e.getRawText());
            out.add(t);
        }
        return out;
    }
}
