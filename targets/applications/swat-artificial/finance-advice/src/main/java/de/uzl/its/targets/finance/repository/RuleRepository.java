package de.uzl.its.targets.finance.repository;

import de.uzl.its.targets.finance.dao.*;
import de.uzl.its.targets.finance.entity.*;
import de.uzl.its.targets.finance.model.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

@Repository
public class RuleRepository {
    private final RuleSetDao setDao;
    private final RuleDao ruleDao;

    public RuleRepository(RuleSetDao s, RuleDao r) {
        this.setDao = s;
        this.ruleDao = r;
    }

    @Transactional
    public RuleSet save(RuleSet rs) {
        setDao.insertRuleset(rs.getId());
        for (int i = 0; i < rs.getRules().size(); i++) {
            Rule rr = rs.getRules().get(i);
            ruleDao.insertRule(rr.getId(), rs.getId(), rr.getExpr());
        }
        return rs;
    }

    public RuleSet find(String id) {
        List<RuleEntity> rules = ruleDao.findByRulesetId(id);
        if (rules == null || rules.isEmpty()) return null;
        RuleSet rs = new RuleSet();
        rs.setId(id);
        List<Rule> list = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            RuleEntity e = rules.get(i);
            list.add(new Rule(e.getId(), e.getExpr()));
        }
        rs.setRules(list);
        return rs;
    }

    public List<RuleSet> findAll() {
        return new ArrayList<>();
    }
}
