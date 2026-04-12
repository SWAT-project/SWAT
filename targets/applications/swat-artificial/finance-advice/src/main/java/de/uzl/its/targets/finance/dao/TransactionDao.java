package de.uzl.its.targets.finance.dao;

import de.uzl.its.targets.finance.entity.TransactionEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.util.*;

@Repository
public interface TransactionDao extends JpaRepository<TransactionEntity, String> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO transactions(id,account_id,date_ms,amount_cents,currency,merchant,category,raw_text) VALUES(?1,?2,?3,?4,?5,?6,?7,?8)", nativeQuery = true)
    void insertTx(String id, String accountId, long dateMs, Long amountCents, String currency, String merchant, String category, String rawText);

    @Query(value = "SELECT * FROM transactions WHERE account_id=?1 ORDER BY date_ms ASC", nativeQuery = true)
    List<TransactionEntity> listByAccount(String accountId);

    @Query(value = "SELECT * FROM transactions WHERE account_id=?1 AND (?2 IS NULL OR date_ms>=?2) AND (?3 IS NULL OR date_ms<=?3) AND (?4 IS NULL OR LOWER(category)=LOWER(?4)) AND (?5 IS NULL OR LOWER(COALESCE(raw_text, merchant,'')) LIKE CONCAT('%',LOWER(?5),'%')) AND (?6 IS NULL OR amount_cents>=?6) AND (?7 IS NULL OR amount_cents<=?7) ORDER BY date_ms ASC", nativeQuery = true)
    List<TransactionEntity> filtered(String accountId, Long fromMs, Long toMs, String category, String q, Long minAmountCents, Long maxAmountCents);
}
