package de.uzl.its.targets.finance.dao;

import de.uzl.its.targets.finance.entity.RuleEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

@Repository
public interface RuleDao extends JpaRepository<RuleEntity, String> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO rules(id,ruleset_id,expr) VALUES(?1,?2,?3)", nativeQuery = true)
    void insertRule(String id, String rulesetId, String expr);

    @Query(value = "SELECT * FROM rules WHERE ruleset_id=?1", nativeQuery = true)
    List<RuleEntity> findByRulesetId(String rulesetId);
}
