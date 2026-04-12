package de.uzl.its.targets.finance_engine.model;

import java.util.ArrayList;
import java.util.List;

public class Account {
    public String id;
    public String userId;
    public String accountType;
    public String currency;

    public List<LinkedInstitution> linkedInstitutions = new ArrayList<LinkedInstitution>();

    // CHANGED: was Map<String,String> metadata; now a list of key/value entries
    public List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();

    public List<Transaction> transactions = new ArrayList<Transaction>();

    public Account() {}

    public Account(String id, String userId, String accountType, String currency) {
        this.id = id;
        this.userId = userId;
        this.accountType = accountType;
        this.currency = currency;
    }
}
