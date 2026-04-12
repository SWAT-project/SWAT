package de.uzl.its.targets.finance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "linked_institutions")
@Data
public class LinkedInstitutionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;
    @Column(name = "account_id")
    private String accountId;
    private String name;
}
