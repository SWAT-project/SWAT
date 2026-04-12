package de.uzl.its.targets.finance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rules")
@Data
public class RuleEntity {
    @Id
    private String id;
    @Column(name = "ruleset_id")
    private String rulesetId;
    private String expr;
}
