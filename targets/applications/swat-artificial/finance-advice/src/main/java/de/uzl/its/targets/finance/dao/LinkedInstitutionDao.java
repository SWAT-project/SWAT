package de.uzl.its.targets.finance.dao;

import de.uzl.its.targets.finance.entity.LinkedInstitutionEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

@Repository
public interface LinkedInstitutionDao extends JpaRepository<LinkedInstitutionEntity, Long> {
    @Query(value = "SELECT name FROM linked_institutions WHERE account_id=?1", nativeQuery = true)
    List<String> findNamesByAccountId(String accountId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO linked_institutions(account_id,name) VALUES(?1,?2)", nativeQuery = true)
    void insertName(String accountId, String name);
}
