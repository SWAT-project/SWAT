package de.uzl.its.targets.finance.repository;

import de.uzl.its.targets.finance.dao.*;
import de.uzl.its.targets.finance.entity.*;
import de.uzl.its.targets.finance.model.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

@Repository
public class AccountRepository {
    private final AccountDao accountDao;
    private final LinkedInstitutionDao liDao;

    public AccountRepository(AccountDao a, LinkedInstitutionDao l) {
        this.accountDao = a;
        this.liDao = l;
    }

    @Transactional
    public Account save(Account a) {
        accountDao.insertAccount(a.getId(), a.getUserId(), a.getAccountType(), a.getCurrency(), a.getMetadata());
        if (a.getLinkedInstitutionNames() != null) {
            for (int i = 0; i < a.getLinkedInstitutionNames().size(); i++) {
                String n = a.getLinkedInstitutionNames().get(i);
                if (n != null && n.length() > 0) liDao.insertName(a.getId(), n);
            }
        }
        return a;
    }

    public Account findById(String id) {
        AccountEntity e = accountDao.findOneById(id);
        if (e == null) return null;
        Account a = new Account();
        a.setId(e.getId());
        a.setUserId(e.getUserId());
        a.setAccountType(e.getAccountType());
        a.setCurrency(e.getCurrency());
        a.setMetadata(e.getMetadata());
        List<String> names = liDao.findNamesByAccountId(id);
        if (names != null) a.setLinkedInstitutionNames(names);
        return a;
    }

    public Collection<Account> findAll() {
        List<AccountEntity> list = accountDao.selectAll();
        List<Account> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            AccountEntity e = list.get(i);
            Account a = new Account();
            a.setId(e.getId());
            a.setUserId(e.getUserId());
            a.setAccountType(e.getAccountType());
            a.setCurrency(e.getCurrency());
            a.setMetadata(e.getMetadata());
            a.setLinkedInstitutionNames(liDao.findNamesByAccountId(e.getId()));
            out.add(a);
        }
        return out;
    }
}
