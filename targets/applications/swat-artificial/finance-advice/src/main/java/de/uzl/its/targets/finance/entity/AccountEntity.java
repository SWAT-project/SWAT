package de.uzl.its.targets.finance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "accounts")
@Data
public class AccountEntity {
    @Id
    private String id;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "account_type")
    private String accountType;
    private String currency;
    private String metadata;
}
