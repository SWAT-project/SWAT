package de.uzl.its.targets.finance.dao;

import de.uzl.its.targets.finance.entity.AccountEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

@Repository
public interface AccountDao extends JpaRepository<AccountEntity, String> {
    @Query(value = "SELECT * FROM accounts WHERE id=?1", nativeQuery = true)
    AccountEntity findOneById(String id);

    @Query(value = "SELECT * FROM accounts", nativeQuery = true)
    List<AccountEntity> selectAll();

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO accounts(id,user_id,account_type,currency,metadata) VALUES(?1,?2,?3,?4,?5)", nativeQuery = true)
    void insertAccount(String id, String userId, String accountType, String currency, String metadata);
}
