package de.uzl.its.targets.finance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transactions")
@Data
public class TransactionEntity {
    @Id
    private String id;
    @Column(name = "account_id")
    private String accountId;
    @Column(name = "date_ms")
    private long dateMs;
    @Column(name = "amount_cents")
    private Long amountCents;
    private String currency;
    private String merchant;
    private String category;
    @Column(name = "raw_text")
    private String rawText;
}
