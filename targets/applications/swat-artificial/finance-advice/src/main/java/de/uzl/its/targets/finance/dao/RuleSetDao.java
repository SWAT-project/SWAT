package de.uzl.its.targets.finance.dao;

import de.uzl.its.targets.finance.entity.RuleSetEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

@Repository
public interface RuleSetDao extends JpaRepository<RuleSetEntity, String> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO rulesets(id) VALUES(?1)", nativeQuery = true)
    void insertRuleset(String id);
}
