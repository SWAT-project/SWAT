package de.uzl.its.targets.finance_engine.model;

import java.time.LocalDate;

public class Transaction {
    public String id;
    public LocalDate date;
    public String merchant;
    public String category;
    public long amountMinor; // minor units (cents)
    public String currency;
    public String raw; // raw description
    public Transaction() {}
    public Transaction(String id, LocalDate date, String merchant, String category, long amountMinor, String currency, String raw) {
        this.id = id;
        this.date = date;
        this.merchant = merchant;
        this.category = category;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.raw = raw;
    }
}
