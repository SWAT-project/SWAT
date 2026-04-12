package de.uzl.its.targets.finance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rulesets")
@Data
public class RuleSetEntity {
    @Id
    private String id;
}
