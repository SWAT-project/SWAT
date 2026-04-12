package de.uzl.its.targets.finance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class Transaction {
    private String id;
    private String accountId;
    private long dateMs; // epoch millis
    private Long amountCents;
    private String currency;
    private String merchant;
    private String category;
    private String rawText;
}
